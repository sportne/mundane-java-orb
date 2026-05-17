package io.github.mundanej.mjo.idl.lexer;

import io.github.mundanej.mjo.common.BoundedLimit;
import java.util.Objects;

/**
 * Bounded scanning options for the IDL lexer.
 *
 * @param sourceLengthLimit maximum source length in Java {@link String} characters
 * @param tokenCountLimit maximum non-EOF tokens emitted
 * @param tokenLengthLimit maximum token lexeme length
 * @param diagnosticCountLimit maximum diagnostics emitted
 */
public record IdlLexerOptions(
    BoundedLimit sourceLengthLimit,
    BoundedLimit tokenCountLimit,
    BoundedLimit tokenLengthLimit,
    BoundedLimit diagnosticCountLimit) {

  /** Creates validated lexer options. */
  public IdlLexerOptions {
    Objects.requireNonNull(sourceLengthLimit, "sourceLengthLimit");
    Objects.requireNonNull(tokenCountLimit, "tokenCountLimit");
    Objects.requireNonNull(tokenLengthLimit, "tokenLengthLimit");
    Objects.requireNonNull(diagnosticCountLimit, "diagnosticCountLimit");
  }

  /** Returns deterministic defaults suitable for normal IDL source validation. */
  public static IdlLexerOptions defaults() {
    return new IdlLexerOptions(
        new BoundedLimit("idl-source-length", 1_048_576L),
        new BoundedLimit("idl-token-count", 262_144L),
        new BoundedLimit("idl-token-length", 65_536L),
        new BoundedLimit("idl-diagnostic-count", 1_024L));
  }
}
