package io.github.mundanej.mjo.idl.parser;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import io.github.mundanej.mjo.idl.ast.IdlTranslationUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result of parsing one IDL translation unit.
 *
 * @param translationUnit parsed AST when no error diagnostics were emitted
 * @param diagnostics lexer, preprocessor, and parser diagnostics in encounter order
 */
public record IdlParseResult(
    Optional<IdlTranslationUnit> translationUnit, List<Diagnostic> diagnostics) {

  /** Creates an immutable parse result. */
  public IdlParseResult {
    Objects.requireNonNull(translationUnit, "translationUnit");
    diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    if (diagnostics.stream()
            .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR)
        && translationUnit.isPresent()) {
      throw new IllegalArgumentException("translationUnit must be empty when errors are present");
    }
  }

  /** Returns whether any emitted diagnostic is an error. */
  public boolean hasErrors() {
    return diagnostics.stream()
        .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
  }
}
