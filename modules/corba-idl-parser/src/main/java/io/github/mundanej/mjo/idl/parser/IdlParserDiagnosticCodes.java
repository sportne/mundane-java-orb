package io.github.mundanej.mjo.idl.parser;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes emitted by the IDL parser. */
public final class IdlParserDiagnosticCodes {

  /** Parser encountered a token that is not valid in the current grammar position. */
  public static final DiagnosticCode UNEXPECTED_TOKEN = new DiagnosticCode("IDL-0300");

  /** Parser reached end of source before a required construct was complete. */
  public static final DiagnosticCode UNEXPECTED_EOF = new DiagnosticCode("IDL-0301");

  /** Parser recognized a valid IDL construct that is outside the approved slice. */
  public static final DiagnosticCode UNSUPPORTED_CONSTRUCT = new DiagnosticCode("IDL-0302");

  /** Parser recognized a type form that is outside the approved slice. */
  public static final DiagnosticCode UNSUPPORTED_TYPE = new DiagnosticCode("IDL-0303");

  /** Parser found a declarator shape that is outside the approved slice. */
  public static final DiagnosticCode UNSUPPORTED_DECLARATOR = new DiagnosticCode("IDL-0304");

  private IdlParserDiagnosticCodes() {}
}
