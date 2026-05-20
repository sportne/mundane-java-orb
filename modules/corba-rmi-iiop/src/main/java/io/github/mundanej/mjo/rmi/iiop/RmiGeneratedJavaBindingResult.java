package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.util.List;
import java.util.Objects;

/**
 * Immutable result of generated Java binding source planning.
 *
 * @param sources generated Java source values when no error diagnostics were emitted
 * @param diagnostics binding-generation diagnostics in deterministic order
 */
public record RmiGeneratedJavaBindingResult(
    List<RmiGeneratedJavaBindingSource> sources, List<Diagnostic> diagnostics) {

  /** Creates an immutable generated binding result. */
  public RmiGeneratedJavaBindingResult {
    sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
    diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    if (diagnostics.stream()
            .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR)
        && !sources.isEmpty()) {
      throw new IllegalArgumentException("sources must be empty when errors are present");
    }
  }

  /** Returns whether any emitted diagnostic is an error. */
  public boolean hasErrors() {
    return diagnostics.stream()
        .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
  }
}
