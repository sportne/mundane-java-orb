package io.github.mundanej.mjo.idl.preprocessor;

import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.idl.lexer.IdlLexerOptions;
import java.util.Objects;

/**
 * Bounded preprocessing options for one IDL translation unit.
 *
 * @param lexerOptions lexer options used for each source after line continuation normalization
 * @param includeDepthLimit maximum recursive include depth
 * @param macroExpansionLimit maximum replacement count per macro expansion pass
 * @param diagnosticCountLimit maximum diagnostics emitted by the preprocessor
 */
public record IdlPreprocessorOptions(
    IdlLexerOptions lexerOptions,
    BoundedLimit includeDepthLimit,
    BoundedLimit macroExpansionLimit,
    BoundedLimit diagnosticCountLimit) {

  /** Creates validated preprocessor options. */
  public IdlPreprocessorOptions {
    Objects.requireNonNull(lexerOptions, "lexerOptions");
    Objects.requireNonNull(includeDepthLimit, "includeDepthLimit");
    Objects.requireNonNull(macroExpansionLimit, "macroExpansionLimit");
    Objects.requireNonNull(diagnosticCountLimit, "diagnosticCountLimit");
  }

  /** Returns deterministic defaults suitable for normal IDL source preprocessing. */
  public static IdlPreprocessorOptions defaults() {
    return new IdlPreprocessorOptions(
        IdlLexerOptions.defaults(),
        new BoundedLimit("idl-include-depth", 64),
        new BoundedLimit("idl-macro-expansions", 16_384),
        new BoundedLimit("idl-preprocessor-diagnostics", 1_024));
  }
}
