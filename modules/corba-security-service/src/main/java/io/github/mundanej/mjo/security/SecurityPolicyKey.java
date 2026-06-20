package io.github.mundanej.mjo.security;

/** Supported Security Service policy keys for the local validation subset. */
public enum SecurityPolicyKey {
  /** Whether authentication is optional or required. */
  AUTHENTICATION,

  /** Whether a credential must chain to a local trust anchor. */
  TRUST,

  /** The required transport protection level advertised to later boundaries. */
  TRANSPORT_PROTECTION,

  /** Whether bounded identity assertion is enabled. */
  IDENTITY_ASSERTION,

  /** Delegation mode. Only disabled delegation is supported. */
  DELEGATION,

  /** Redacted audit recording level. */
  AUDIT
}
