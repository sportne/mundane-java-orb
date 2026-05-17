package io.github.mundanej.mjo.idl.lexer;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes emitted by the IDL lexer. */
public final class IdlDiagnosticCodes {

  /** Invalid character outside a valid IDL token. */
  public static final DiagnosticCode INVALID_CHARACTER = new DiagnosticCode("IDL-0100");

  /** Identifier collides with an IDL keyword because only case differs. */
  public static final DiagnosticCode KEYWORD_CASE_COLLISION = new DiagnosticCode("IDL-0101");

  /** Block comment reached end of source before its closing delimiter. */
  public static final DiagnosticCode UNTERMINATED_BLOCK_COMMENT = new DiagnosticCode("IDL-0102");

  /** String literal reached newline or end of source before closing quote. */
  public static final DiagnosticCode UNTERMINATED_STRING_LITERAL = new DiagnosticCode("IDL-0103");

  /** Character literal reached newline or end of source before closing quote. */
  public static final DiagnosticCode UNTERMINATED_CHARACTER_LITERAL =
      new DiagnosticCode("IDL-0104");

  /** Escape sequence is not defined by OMG IDL lexical rules. */
  public static final DiagnosticCode INVALID_ESCAPE_SEQUENCE = new DiagnosticCode("IDL-0105");

  /** Numeric literal is malformed for its token class. */
  public static final DiagnosticCode INVALID_NUMERIC_LITERAL = new DiagnosticCode("IDL-0106");

  /** Source length exceeded the configured lexer bound. */
  public static final DiagnosticCode SOURCE_LIMIT_EXCEEDED = new DiagnosticCode("IDL-0107");

  /** Token count exceeded the configured lexer bound. */
  public static final DiagnosticCode TOKEN_LIMIT_EXCEEDED = new DiagnosticCode("IDL-0108");

  /** Token length exceeded the configured lexer bound. */
  public static final DiagnosticCode TOKEN_LENGTH_LIMIT_EXCEEDED = new DiagnosticCode("IDL-0109");

  /** Diagnostic count exceeded the configured lexer bound. */
  public static final DiagnosticCode DIAGNOSTIC_LIMIT_EXCEEDED = new DiagnosticCode("IDL-0110");

  /** String or character literal contains a NUL character. */
  public static final DiagnosticCode NUL_LITERAL_CHARACTER = new DiagnosticCode("IDL-0111");

  private IdlDiagnosticCodes() {}
}
