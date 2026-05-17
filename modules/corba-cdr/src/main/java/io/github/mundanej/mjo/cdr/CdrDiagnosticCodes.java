package io.github.mundanej.mjo.cdr;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for CDR encoding and decoding failures. */
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

  /** Length-bearing CDR value used an invalid length. */
  public static final DiagnosticCode INVALID_LENGTH = new DiagnosticCode("CDR-0007");

  /** Length-bearing CDR value exceeded its configured bound. */
  public static final DiagnosticCode LENGTH_LIMIT_EXCEEDED = new DiagnosticCode("CDR-0008");

  /** Narrow string payload was not terminated as required by CDR. */
  public static final DiagnosticCode MALFORMED_STRING = new DiagnosticCode("CDR-0009");

  /** Encapsulation byte-order marker was not one of the CDR marker values. */
  public static final DiagnosticCode INVALID_ENCAPSULATION_BYTE_ORDER =
      new DiagnosticCode("CDR-0010");

  /** Sequence or fixed-array size was invalid for generated-code loops. */
  public static final DiagnosticCode INVALID_COLLECTION_SIZE = new DiagnosticCode("CDR-0011");

  private CdrDiagnosticCodes() {}
}
