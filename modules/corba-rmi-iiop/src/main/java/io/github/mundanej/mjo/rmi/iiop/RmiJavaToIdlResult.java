package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result of Java-to-IDL model mapping.
 *
 * @param translationUnit mapped IDL model when no error diagnostics were emitted
 * @param diagnostics eligibility and mapping diagnostics in deterministic order
 */
public record RmiJavaToIdlResult(
    Optional<RmiIdlTranslationUnit> translationUnit, List<Diagnostic> diagnostics) {

  /** Creates an immutable Java-to-IDL mapping result. */
  public RmiJavaToIdlResult {
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
