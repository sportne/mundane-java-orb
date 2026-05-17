package io.github.mundanej.mjo.giop;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for GIOP message failures. */
public final class GiopDiagnosticCodes {

  /** GIOP input ended before the fixed header or declared body was available. */
  public static final DiagnosticCode TRUNCATED_MESSAGE = new DiagnosticCode("GIOP-0001");

  /** The message did not start with the GIOP magic octets. */
  public static final DiagnosticCode INVALID_MAGIC = new DiagnosticCode("GIOP-0002");

  /** The GIOP version is not supported by this slice. */
  public static final DiagnosticCode UNSUPPORTED_VERSION = new DiagnosticCode("GIOP-0003");

  /** The message type octet is not recognized. */
  public static final DiagnosticCode UNKNOWN_MESSAGE_TYPE = new DiagnosticCode("GIOP-0004");

  /** The flags octet contains unsupported bits. */
  public static final DiagnosticCode INVALID_FLAGS = new DiagnosticCode("GIOP-0005");

  /** The declared message size does not match the available body. */
  public static final DiagnosticCode MESSAGE_SIZE_MISMATCH = new DiagnosticCode("GIOP-0006");

  /** A configured message, body, or service-context bound rejected the input. */
  public static final DiagnosticCode LIMIT_EXCEEDED = new DiagnosticCode("GIOP-0007");

  /** A message body contains unsupported syntax for this slice. */
  public static final DiagnosticCode UNSUPPORTED_BODY = new DiagnosticCode("GIOP-0008");

  /** A message body contains invalid fields for its message kind. */
  public static final DiagnosticCode INVALID_BODY = new DiagnosticCode("GIOP-0009");

  private GiopDiagnosticCodes() {}
}
