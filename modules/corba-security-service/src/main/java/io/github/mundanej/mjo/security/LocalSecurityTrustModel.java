package io.github.mundanej.mjo.security;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** In-memory credential/trust model for the supported local Security Service subset. */
public final class LocalSecurityTrustModel {

  private final SecurityServiceOptions options;
  private final Clock clock;
  private final Map<String, SecurityCredentialSnapshot> credentials = new LinkedHashMap<>();
  private final Map<String, SecurityTrustAnchorSnapshot> trustAnchors = new LinkedHashMap<>();

  /** Creates a model with default local Security Service limits. */
  public LocalSecurityTrustModel() {
    this(SecurityServiceOptions.defaults());
  }

  /** Creates a model with caller-provided local Security Service limits. */
  public LocalSecurityTrustModel(SecurityServiceOptions options) {
    this(options, Clock.systemUTC());
  }

  /** Creates a model with caller-provided limits and clock. */
  public LocalSecurityTrustModel(SecurityServiceOptions options, Clock clock) {
    this.options = Objects.requireNonNull(options, "options");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /** Registers a trust anchor for one credential kind. */
  public synchronized SecurityTrustAnchorSnapshot registerTrustAnchor(
      String trustAnchorId, String issuerPrincipalId, SecurityCredentialKind credentialKind) {
    String anchorId = SecurityNames.requireIdentifier(trustAnchorId, "trust-anchor ID", options);
    if (trustAnchors.containsKey(anchorId)) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.TRUST_ANCHOR_ALREADY_EXISTS,
          "trust anchor already exists: " + anchorId);
    }
    if (trustAnchors.size() >= options.maxTrustAnchors()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.TRUST_ANCHOR_LIMIT_EXCEEDED,
          "trust model has reached " + options.maxTrustAnchors() + " trust anchors");
    }
    SecurityTrustAnchorSnapshot snapshot =
        new SecurityTrustAnchorSnapshot(
            new SecurityTrustAnchorId(anchorId),
            new PrincipalId(
                SecurityNames.requireIdentifier(issuerPrincipalId, "issuer principal ID", options)),
            Objects.requireNonNull(credentialKind, "credentialKind"));
    trustAnchors.put(anchorId, snapshot);
    return snapshot;
  }

  /** Removes a trust anchor by ID. */
  public synchronized SecurityTrustAnchorSnapshot removeTrustAnchor(String trustAnchorId) {
    String anchorId = SecurityNames.requireIdentifier(trustAnchorId, "trust-anchor ID", options);
    SecurityTrustAnchorSnapshot removed = trustAnchors.remove(anchorId);
    if (removed == null) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.TRUST_ANCHOR_NOT_FOUND,
          "unknown trust anchor: " + anchorId);
    }
    return removed;
  }

  /** Looks up a trust anchor by ID. */
  public synchronized Optional<SecurityTrustAnchorSnapshot> lookupTrustAnchor(
      String trustAnchorId) {
    String anchorId = SecurityNames.requireIdentifier(trustAnchorId, "trust-anchor ID", options);
    return Optional.ofNullable(trustAnchors.get(anchorId));
  }

  /** Registers a bounded local credential snapshot. */
  public synchronized SecurityCredentialSnapshot registerCredential(
      String credentialId,
      String principalId,
      SecurityCredentialKind kind,
      String trustAnchorId,
      Instant issuedAt,
      Instant expiresAt) {
    String id = SecurityNames.requireIdentifier(credentialId, "credential ID", options);
    if (credentials.containsKey(id)) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.CREDENTIAL_ALREADY_EXISTS,
          "credential already exists: " + id);
    }
    if (credentials.size() >= options.maxCredentials()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.CREDENTIAL_LIMIT_EXCEEDED,
          "trust model has reached " + options.maxCredentials() + " credentials");
    }
    SecurityCredentialSnapshot snapshot =
        new SecurityCredentialSnapshot(
            new SecurityCredentialId(id),
            new PrincipalId(SecurityNames.requireIdentifier(principalId, "principal ID", options)),
            kind,
            new SecurityTrustAnchorId(
                SecurityNames.requireIdentifier(trustAnchorId, "trust-anchor ID", options)),
            issuedAt,
            expiresAt);
    credentials.put(id, snapshot);
    return snapshot;
  }

  /** Removes a credential by ID. */
  public synchronized SecurityCredentialSnapshot removeCredential(String credentialId) {
    String id = SecurityNames.requireIdentifier(credentialId, "credential ID", options);
    SecurityCredentialSnapshot removed = credentials.remove(id);
    if (removed == null) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.CREDENTIAL_NOT_FOUND, "unknown credential: " + id);
    }
    return removed;
  }

  /** Looks up a credential by ID. */
  public synchronized Optional<SecurityCredentialSnapshot> lookupCredential(String credentialId) {
    String id = SecurityNames.requireIdentifier(credentialId, "credential ID", options);
    return Optional.ofNullable(credentials.get(id));
  }

  /** Creates a bounded trust-evaluation input for a registered credential. */
  public synchronized SecurityTrustEvaluationInput evaluationInput(String credentialId) {
    String id = SecurityNames.requireIdentifier(credentialId, "credential ID", options);
    SecurityCredentialSnapshot credential = credentials.get(id);
    if (credential == null) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.CREDENTIAL_NOT_FOUND, "unknown credential: " + id);
    }
    return SecurityTrustEvaluationInput.from(credential);
  }

  /** Evaluates a registered credential against the local trust-anchor set. */
  public synchronized SecurityTrustDecision evaluateCredential(String credentialId) {
    return evaluate(evaluationInput(credentialId));
  }

  /** Evaluates project-owned credential/trust metadata against the local trust-anchor set. */
  public synchronized SecurityTrustDecision evaluate(SecurityTrustEvaluationInput input) {
    Objects.requireNonNull(input, "input");
    SecurityNames.requireIdentifier(input.credentialId().value(), "credential ID", options);
    SecurityNames.requireIdentifier(input.principalId().value(), "principal ID", options);
    SecurityNames.requireIdentifier(input.trustAnchorId().value(), "trust-anchor ID", options);
    Instant now = clock.instant();
    SecurityLifetimes.requireValidAt(input, now);
    SecurityTrustAnchorSnapshot anchor = trustAnchors.get(input.trustAnchorId().value());
    if (anchor == null || anchor.credentialKind() != input.kind()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.CREDENTIAL_UNTRUSTED,
          "credential is not trusted by local anchors: " + input.credentialId().value());
    }
    return new SecurityTrustDecision(
        input.credentialId(), input.principalId(), input.trustAnchorId(), now);
  }

  /** Lists credentials in deterministic registration order. */
  public synchronized List<SecurityCredentialSnapshot> listCredentials() {
    return credentials.values().stream().toList();
  }

  /** Lists trust anchors in deterministic registration order. */
  public synchronized List<SecurityTrustAnchorSnapshot> listTrustAnchors() {
    return trustAnchors.values().stream().toList();
  }

  /** Returns an immutable deterministic snapshot of the model. */
  public synchronized SecurityTrustModelSnapshot snapshot() {
    return new SecurityTrustModelSnapshot(listCredentials(), listTrustAnchors());
  }
}
