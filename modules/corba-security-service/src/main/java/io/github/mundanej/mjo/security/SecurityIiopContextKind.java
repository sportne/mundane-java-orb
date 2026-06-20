package io.github.mundanej.mjo.security;

/** Supported local IIOP carrier kinds for CSIv2 metadata. */
public enum SecurityIiopContextKind {
  /** GIOP request service context carrying local CSIv2 metadata. */
  SERVICE_CONTEXT,

  /** IOR tagged component carrying local CSIv2 metadata. */
  TAGGED_COMPONENT
}
