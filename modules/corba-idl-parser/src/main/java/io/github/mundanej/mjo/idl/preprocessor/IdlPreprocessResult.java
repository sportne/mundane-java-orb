package io.github.mundanej.mjo.idl.preprocessor;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import io.github.mundanej.mjo.idl.lexer.IdlToken;
import java.util.List;
import java.util.Objects;

/**
 * Immutable result of preprocessing one IDL translation unit.
 *
 * @param tokens token stream including one final EOF token
 * @param diagnostics diagnostics emitted while preprocessing and lexing
 * @param includedSourceNames source names successfully included in encounter order
 */
public record IdlPreprocessResult(
    List<IdlToken> tokens, List<Diagnostic> diagnostics, List<String> includedSourceNames) {

  /** Creates an immutable preprocessor result. */
  public IdlPreprocessResult {
    tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
    diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    includedSourceNames =
        List.copyOf(Objects.requireNonNull(includedSourceNames, "includedSourceNames"));
  }

  /** Returns whether any emitted diagnostic is an error. */
  public boolean hasErrors() {
    return diagnostics.stream()
        .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
  }
}
