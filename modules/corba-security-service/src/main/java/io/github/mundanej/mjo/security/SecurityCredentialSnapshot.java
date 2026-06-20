package io.github.mundanej.mjo.security;

import java.time.Instant;
import java.util.Objects;

/** Immutable snapshot of one local Security Service credential. */
public record SecurityCredentialSnapshot(
    SecurityCredentialId credentialId,
    PrincipalId principalId,
    SecurityCredentialKind kind,
    SecurityTrustAnchorId trustAnchorId,
    Instant issuedAt,
    Instant expiresAt) {

  /** Creates a validated credential snapshot. */
  public SecurityCredentialSnapshot {
    Objects.requireNonNull(credentialId, "credentialId");
    Objects.requireNonNull(principalId, "principalId");
    if (kind == null) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_IDENTIFIER, "credential kind must not be null");
    }
    Objects.requireNonNull(trustAnchorId, "trustAnchorId");
    SecurityLifetimes.requireValidLifetime(issuedAt, expiresAt);
  }
}
