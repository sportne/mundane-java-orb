package io.github.mundanej.mjo.idl.semantics;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result of IDL semantic analysis.
 *
 * @param model semantic model when no error diagnostics were emitted
 * @param diagnostics semantic diagnostics in encounter order
 */
public record IdlSemanticResult(Optional<IdlSemanticModel> model, List<Diagnostic> diagnostics) {

  /** Creates an immutable semantic result. */
  public IdlSemanticResult {
    Objects.requireNonNull(model, "model");
    diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    if (diagnostics.stream()
            .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR)
        && model.isPresent()) {
      throw new IllegalArgumentException("model must be empty when errors are present");
    }
  }

  /** Returns whether any emitted diagnostic is an error. */
  public boolean hasErrors() {
    return diagnostics.stream()
        .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
  }
}
