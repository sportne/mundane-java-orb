package io.github.mundanej.mjo.security;

import java.time.Instant;
import java.util.Objects;

/** Project-owned bounded input for local Security Service trust evaluation. */
public record SecurityTrustEvaluationInput(
    SecurityCredentialId credentialId,
    PrincipalId principalId,
    SecurityCredentialKind kind,
    SecurityTrustAnchorId trustAnchorId,
    Instant issuedAt,
    Instant expiresAt) {

  /** Creates a validated trust evaluation input. */
  public SecurityTrustEvaluationInput {
    Objects.requireNonNull(credentialId, "credentialId");
    Objects.requireNonNull(principalId, "principalId");
    if (kind == null) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_IDENTIFIER, "credential kind must not be null");
    }
    Objects.requireNonNull(trustAnchorId, "trustAnchorId");
    SecurityLifetimes.requireValidLifetime(issuedAt, expiresAt);
  }

  /** Creates evaluation input from an immutable credential snapshot. */
  public static SecurityTrustEvaluationInput from(SecurityCredentialSnapshot credential) {
    Objects.requireNonNull(credential, "credential");
    return new SecurityTrustEvaluationInput(
        credential.credentialId(),
        credential.principalId(),
        credential.kind(),
        credential.trustAnchorId(),
        credential.issuedAt(),
        credential.expiresAt());
  }
}
