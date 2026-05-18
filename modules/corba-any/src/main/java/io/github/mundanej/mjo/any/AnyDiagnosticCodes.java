package io.github.mundanej.mjo.any;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for local Any failures. */
public final class AnyDiagnosticCodes {

  /** A codec was used with a value or TypeCode of the wrong kind. */
  public static final DiagnosticCode TYPE_MISMATCH = new DiagnosticCode("ANY-0001");

  /** An aggregate value was missing a required member. */
  public static final DiagnosticCode MISSING_MEMBER = new DiagnosticCode("ANY-0002");

  /** An aggregate value contained a member not present in the TypeCode. */
  public static final DiagnosticCode UNKNOWN_MEMBER = new DiagnosticCode("ANY-0003");

  /** The requested TypeCode kind is outside the local Any slice. */
  public static final DiagnosticCode UNSUPPORTED_TYPE = new DiagnosticCode("ANY-0004");

  /** A CDR enum ordinal or local enum label was outside the TypeCode constants. */
  public static final DiagnosticCode INVALID_ENUM_VALUE = new DiagnosticCode("ANY-0005");

  private AnyDiagnosticCodes() {}
}
