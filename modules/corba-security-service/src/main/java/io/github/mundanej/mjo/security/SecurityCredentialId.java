package io.github.mundanej.mjo.security;

/** Stable local identifier for a Security Service credential. */
public record SecurityCredentialId(String value) {

  /** Creates a validated credential identifier. */
  public SecurityCredentialId {
    value =
        SecurityNames.requireIdentifier(
            value, "credential ID", SecurityServiceOptions.modelLimits());
  }
}
