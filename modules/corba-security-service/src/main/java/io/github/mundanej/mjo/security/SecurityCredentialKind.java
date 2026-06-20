package io.github.mundanej.mjo.security;

/** Explicit credential categories supported by the local Security Service model. */
public enum SecurityCredentialKind {
  /** Credential identifies a local or remote user principal. */
  USER,

  /** Credential identifies a local service principal. */
  SERVICE,

  /** Credential identifies a bounded identity assertion. */
  ASSERTION
}
