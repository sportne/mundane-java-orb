package io.github.mundanej.mjo.idl.lexer;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.util.List;
import java.util.Objects;

/**
 * Immutable result of lexing one IDL source.
 *
 * @param tokens token stream including the final EOF token
 * @param diagnostics diagnostics emitted while scanning
 */
public record IdlLexResult(List<IdlToken> tokens, List<Diagnostic> diagnostics) {

  /** Creates an immutable lexer result. */
  public IdlLexResult {
    tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
    diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
  }

  /** Returns whether any emitted diagnostic is an error. */
  public boolean hasErrors() {
    return diagnostics.stream()
        .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
  }
}
