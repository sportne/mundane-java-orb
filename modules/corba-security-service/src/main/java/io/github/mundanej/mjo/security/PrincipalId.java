package io.github.mundanej.mjo.security;

/** Stable local identifier for a Security Service principal. */
public record PrincipalId(String value) {

  /** Creates a validated principal identifier. */
  public PrincipalId {
    value =
        SecurityNames.requireIdentifier(
            value, "principal ID", SecurityServiceOptions.modelLimits());
  }
}
