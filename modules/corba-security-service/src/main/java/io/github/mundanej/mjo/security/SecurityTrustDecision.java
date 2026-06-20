package io.github.mundanej.mjo.security;

import java.time.Instant;
import java.util.Objects;

/** Deterministic result for a trusted local credential evaluation. */
public record SecurityTrustDecision(
    SecurityCredentialId credentialId,
    PrincipalId principalId,
    SecurityTrustAnchorId trustAnchorId,
    Instant evaluatedAt) {

  /** Creates a validated trust decision. */
  public SecurityTrustDecision {
    Objects.requireNonNull(credentialId, "credentialId");
    Objects.requireNonNull(principalId, "principalId");
    Objects.requireNonNull(trustAnchorId, "trustAnchorId");
    Objects.requireNonNull(evaluatedAt, "evaluatedAt");
  }
}
