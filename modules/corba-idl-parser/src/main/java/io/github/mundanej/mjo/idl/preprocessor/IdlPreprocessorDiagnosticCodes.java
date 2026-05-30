package io.github.mundanej.mjo.idl.preprocessor;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes emitted by the IDL preprocessor. */
public final class IdlPreprocessorDiagnosticCodes {

  /** Include directive is malformed. */
  public static final DiagnosticCode MALFORMED_INCLUDE = new DiagnosticCode("IDL-0200");

  /** Include path was rejected as unsafe. */
  public static final DiagnosticCode UNSAFE_INCLUDE_PATH = new DiagnosticCode("IDL-0201");

  /** Include source could not be resolved. */
  public static final DiagnosticCode INCLUDE_NOT_FOUND = new DiagnosticCode("IDL-0202");

  /** Include expansion exceeded the configured depth bound. */
  public static final DiagnosticCode INCLUDE_DEPTH_EXCEEDED = new DiagnosticCode("IDL-0203");

  /** Include cycle was detected. */
  public static final DiagnosticCode INCLUDE_CYCLE = new DiagnosticCode("IDL-0204");

  /** Macro directive is malformed. */
  public static final DiagnosticCode MALFORMED_MACRO = new DiagnosticCode("IDL-0205");

  /** Macro definition replaced an existing definition. */
  public static final DiagnosticCode MACRO_REDEFINED = new DiagnosticCode("IDL-0206");

  /** Macro expansion exceeded the configured bound. */
  public static final DiagnosticCode MACRO_EXPANSION_LIMIT_EXCEEDED =
      new DiagnosticCode("IDL-0207");

  /** Macro expansion recursed into an already expanding macro. */
  public static final DiagnosticCode RECURSIVE_MACRO = new DiagnosticCode("IDL-0208");

  /** Preprocessor token-paste or stringification operator is not implemented in this slice. */
  public static final DiagnosticCode UNSUPPORTED_MACRO_OPERATOR = new DiagnosticCode("IDL-0209");

  /** Conditional directive is malformed or unmatched. */
  public static final DiagnosticCode MALFORMED_CONDITIONAL = new DiagnosticCode("IDL-0210");

  /** Conditional expression is outside the supported first-slice subset. */
  public static final DiagnosticCode UNSUPPORTED_CONDITIONAL_EXPRESSION =
      new DiagnosticCode("IDL-0211");

  /** Conditional group reached end of source before a matching endif. */
  public static final DiagnosticCode UNTERMINATED_CONDITIONAL = new DiagnosticCode("IDL-0212");

  /** Preprocessor directive is not supported in this slice. */
  public static final DiagnosticCode UNSUPPORTED_DIRECTIVE = new DiagnosticCode("IDL-0213");

  /** Preprocessor diagnostics exceeded the configured bound. */
  public static final DiagnosticCode DIAGNOSTIC_LIMIT_EXCEEDED = new DiagnosticCode("IDL-0214");

  /** Line marker directive is malformed. */
  public static final DiagnosticCode MALFORMED_LINE_MARKER = new DiagnosticCode("IDL-0215");

  private IdlPreprocessorDiagnosticCodes() {}
}
