package io.github.mundanej.mjo.security;

/** Stable local identifier for a Security Service trust anchor. */
public record SecurityTrustAnchorId(String value) {

  /** Creates a validated trust-anchor identifier. */
  public SecurityTrustAnchorId {
    value =
        SecurityNames.requireIdentifier(
            value, "trust-anchor ID", SecurityServiceOptions.modelLimits());
  }
}
