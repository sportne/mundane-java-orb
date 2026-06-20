package io.github.mundanej.mjo.security;

import java.util.List;

/** Immutable snapshot of local Security Service credential and trust-anchor state. */
public record SecurityTrustModelSnapshot(
    List<SecurityCredentialSnapshot> credentials, List<SecurityTrustAnchorSnapshot> trustAnchors) {

  /** Creates a snapshot with immutable deterministic lists. */
  public SecurityTrustModelSnapshot {
    credentials = List.copyOf(credentials);
    trustAnchors = List.copyOf(trustAnchors);
  }
}
