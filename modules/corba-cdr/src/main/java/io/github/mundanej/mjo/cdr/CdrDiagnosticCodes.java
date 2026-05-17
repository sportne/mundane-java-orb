package io.github.mundanej.mjo.cdr;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for CDR primitive encoding and decoding failures. */
public final class CdrDiagnosticCodes {

  /** Input ended before the aligned primitive value was fully available. */
  public static final DiagnosticCode TRUNCATED_INPUT = new DiagnosticCode("CDR-0001");

  /** Output would exceed the configured writer bound. */
  public static final DiagnosticCode OUTPUT_LIMIT_EXCEEDED = new DiagnosticCode("CDR-0002");

  /** Boolean octet was not one of the CDR boolean values `0` or `1`. */
  public static final DiagnosticCode INVALID_BOOLEAN = new DiagnosticCode("CDR-0003");

  /** Character value does not fit in the first primitive CDR character slice. */
  public static final DiagnosticCode INVALID_CHARACTER = new DiagnosticCode("CDR-0004");

  /** Unsigned primitive value does not fit in its CDR wire width. */
  public static final DiagnosticCode UNSIGNED_VALUE_OUT_OF_RANGE = new DiagnosticCode("CDR-0005");

  /** Long double raw payload was not exactly 16 octets. */
  public static final DiagnosticCode INVALID_LONG_DOUBLE = new DiagnosticCode("CDR-0006");

  private CdrDiagnosticCodes() {}
}
