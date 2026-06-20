package io.github.mundanej.mjo.security;

import java.util.Objects;

/** Immutable snapshot of one accepted local Security Service trust anchor. */
public record SecurityTrustAnchorSnapshot(
    SecurityTrustAnchorId trustAnchorId,
    PrincipalId issuerPrincipalId,
    SecurityCredentialKind credentialKind) {

  /** Creates a validated trust-anchor snapshot. */
  public SecurityTrustAnchorSnapshot {
    Objects.requireNonNull(trustAnchorId, "trustAnchorId");
    Objects.requireNonNull(issuerPrincipalId, "issuerPrincipalId");
    Objects.requireNonNull(credentialKind, "credentialKind");
  }
}
