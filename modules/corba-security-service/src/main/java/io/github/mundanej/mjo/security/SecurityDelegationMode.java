package io.github.mundanej.mjo.security;

/** Delegation policy values. */
public enum SecurityDelegationMode {
  /** Delegation is disabled, which is the only supported local mode. */
  DISABLED,

  /** Delegation was requested and must be rejected by the policy model. */
  ENABLED
}
