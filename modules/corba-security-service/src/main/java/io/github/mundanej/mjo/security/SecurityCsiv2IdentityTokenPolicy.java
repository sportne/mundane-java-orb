package io.github.mundanej.mjo.security;

/** Supported CSIv2 identity-token policy labels for local metadata validation. */
public enum SecurityCsiv2IdentityTokenPolicy {
  /** No identity token is advertised. */
  ABSENT,

  /** Anonymous identity is advertised without credential discovery. */
  ANONYMOUS,

  /** Principal-name identity assertion metadata is advertised. */
  PRINCIPAL_NAME
}
