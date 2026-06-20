package io.github.mundanej.mjo.security;

/** Trust requirement policy values. */
public enum SecurityTrustRequirement {
  /** Trust evaluation is not required. */
  OPTIONAL,

  /** Credentials must chain to local trust anchors. */
  REQUIRED
}
