package io.github.mundanej.mjo.security;

/** Authentication requirement policy values. */
public enum SecurityAuthenticationRequirement {
  /** Authentication may be absent for this local subset. */
  OPTIONAL,

  /** Authentication must be present for this local subset. */
  REQUIRED
}
