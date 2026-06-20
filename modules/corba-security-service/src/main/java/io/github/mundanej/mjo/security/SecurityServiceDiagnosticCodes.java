package io.github.mundanej.mjo.security;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for supported Security Service failures. */
public final class SecurityServiceDiagnosticCodes {

  /** A configured Security Service limit was outside the supported range. */
  public static final DiagnosticCode INVALID_LIMIT = new DiagnosticCode("SEC-0001");

  /** A principal, credential, or trust-anchor identifier was blank, missing, or oversized. */
  public static final DiagnosticCode MALFORMED_IDENTIFIER = new DiagnosticCode("SEC-0002");

  /** Credential lifetime metadata was malformed or outside the supported local subset. */
  public static final DiagnosticCode MALFORMED_LIFETIME = new DiagnosticCode("SEC-0003");

  /** A credential registration used an ID that already exists. */
  public static final DiagnosticCode CREDENTIAL_ALREADY_EXISTS = new DiagnosticCode("SEC-0004");

  /** A credential lookup or mutation referenced an unknown credential. */
  public static final DiagnosticCode CREDENTIAL_NOT_FOUND = new DiagnosticCode("SEC-0005");

  /** The configured credential count has been reached. */
  public static final DiagnosticCode CREDENTIAL_LIMIT_EXCEEDED = new DiagnosticCode("SEC-0006");

  /** A trust-anchor registration used an ID that already exists. */
  public static final DiagnosticCode TRUST_ANCHOR_ALREADY_EXISTS = new DiagnosticCode("SEC-0007");

  /** A trust-anchor lookup or mutation referenced an unknown anchor. */
  public static final DiagnosticCode TRUST_ANCHOR_NOT_FOUND = new DiagnosticCode("SEC-0008");

  /** The configured trust-anchor count has been reached. */
  public static final DiagnosticCode TRUST_ANCHOR_LIMIT_EXCEEDED = new DiagnosticCode("SEC-0009");

  /** Credential lifetime metadata is not valid at the evaluation instant. */
  public static final DiagnosticCode CREDENTIAL_EXPIRED = new DiagnosticCode("SEC-0010");

  /** A credential does not chain to an accepted local trust anchor. */
  public static final DiagnosticCode CREDENTIAL_UNTRUSTED = new DiagnosticCode("SEC-0011");

  /** A configured Security Service policy setting count exceeded supported bounds. */
  public static final DiagnosticCode POLICY_LIMIT_EXCEEDED = new DiagnosticCode("SEC-0012");

  /** A policy key or value was malformed or outside the supported local subset. */
  public static final DiagnosticCode MALFORMED_POLICY = new DiagnosticCode("SEC-0013");

  /** A policy list declared the same policy key more than once. */
  public static final DiagnosticCode DUPLICATE_POLICY = new DiagnosticCode("SEC-0014");

  /** A policy list combined settings that the supported local subset cannot satisfy. */
  public static final DiagnosticCode CONFLICTING_POLICY = new DiagnosticCode("SEC-0015");

  /** A policy requested delegation, which is outside the supported local subset. */
  public static final DiagnosticCode UNSUPPORTED_DELEGATION = new DiagnosticCode("SEC-0016");

  /** CSIv2 metadata was malformed or outside the supported local subset. */
  public static final DiagnosticCode MALFORMED_CSIV2_METADATA = new DiagnosticCode("SEC-0017");

  /** CSIv2 metadata exceeded a configured size or count bound. */
  public static final DiagnosticCode CSIV2_METADATA_LIMIT_EXCEEDED = new DiagnosticCode("SEC-0018");

  /** CSIv2 mechanism identity is outside the supported local subset. */
  public static final DiagnosticCode UNSUPPORTED_CSIV2_MECHANISM = new DiagnosticCode("SEC-0019");

  /** A policy requires credentials but the local evaluation input did not include one. */
  public static final DiagnosticCode MISSING_CREDENTIAL = new DiagnosticCode("SEC-0020");

  /** A policy requires stronger transport protection than the local metadata provides. */
  public static final DiagnosticCode INSUFFICIENT_TRANSPORT_PROTECTION =
      new DiagnosticCode("SEC-0021");

  /** A local policy evaluation decision or reason was malformed. */
  public static final DiagnosticCode MALFORMED_POLICY_EVALUATION = new DiagnosticCode("SEC-0022");

  /** A local policy evaluation reason exceeded supported bounds. */
  public static final DiagnosticCode POLICY_EVALUATION_LIMIT_EXCEEDED =
      new DiagnosticCode("SEC-0023");

  /** A redacted audit event or failure disclosure was malformed. */
  public static final DiagnosticCode MALFORMED_AUDIT_EVENT = new DiagnosticCode("SEC-0024");

  /** A redacted audit event exceeded supported field or disclosure bounds. */
  public static final DiagnosticCode AUDIT_EVENT_LIMIT_EXCEEDED = new DiagnosticCode("SEC-0025");

  /** A CSIv2 IIOP service context or tagged component was malformed. */
  public static final DiagnosticCode MALFORMED_IIOP_SECURITY_CONTEXT =
      new DiagnosticCode("SEC-0026");

  /** A local Security Service IIOP boundary was used after close. */
  public static final DiagnosticCode IIOP_SECURITY_BOUNDARY_CLOSED = new DiagnosticCode("SEC-0027");

  private SecurityServiceDiagnosticCodes() {}
}
