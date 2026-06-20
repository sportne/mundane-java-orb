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

  private SecurityServiceDiagnosticCodes() {}
}
