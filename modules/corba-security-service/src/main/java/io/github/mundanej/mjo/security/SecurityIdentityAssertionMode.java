package io.github.mundanej.mjo.security;

/** Identity assertion policy values for the supported local subset. */
public enum SecurityIdentityAssertionMode {
  /** Identity assertion is disabled. */
  DISABLED,

  /** A caller may assert only its own authenticated identity. */
  SELF
}
