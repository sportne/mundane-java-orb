package io.github.mundanej.mjo.security;

/** Transport protection policy values advertised by the local policy model. */
public enum SecurityTransportProtection {
  /** No transport protection is required by the policy. */
  NONE,

  /** Integrity protection is required by the policy. */
  INTEGRITY,

  /** Confidentiality protection is required by the policy. */
  CONFIDENTIALITY
}
