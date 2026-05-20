package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result of generated IDL fixture production.
 *
 * @param fixture generated IDL fixture when no error diagnostics were emitted
 * @param diagnostics fixture diagnostics in deterministic order
 */
public record RmiGeneratedIdlResult(
    Optional<RmiGeneratedIdlFixture> fixture, List<Diagnostic> diagnostics) {

  /** Creates an immutable generated IDL result. */
  public RmiGeneratedIdlResult {
    Objects.requireNonNull(fixture, "fixture");
    diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    if (diagnostics.stream()
            .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR)
        && fixture.isPresent()) {
      throw new IllegalArgumentException("fixture must be empty when errors are present");
    }
  }

  /** Returns whether any emitted diagnostic is an error. */
  public boolean hasErrors() {
    return diagnostics.stream()
        .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
  }
}
