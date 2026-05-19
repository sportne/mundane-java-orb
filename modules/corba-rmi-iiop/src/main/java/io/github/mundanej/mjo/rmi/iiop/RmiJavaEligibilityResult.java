package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result of RMI Java declaration eligibility checking.
 *
 * @param remoteInterface validated declaration when no error diagnostics were emitted
 * @param diagnostics diagnostics in deterministic encounter order
 */
public record RmiJavaEligibilityResult(
    Optional<RmiJavaRemoteInterface> remoteInterface, List<Diagnostic> diagnostics) {

  /** Creates an immutable eligibility result. */
  public RmiJavaEligibilityResult {
    Objects.requireNonNull(remoteInterface, "remoteInterface");
    diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    if (diagnostics.stream()
            .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR)
        && remoteInterface.isPresent()) {
      throw new IllegalArgumentException("remoteInterface must be empty when errors are present");
    }
  }

  /** Returns whether any emitted diagnostic is an error. */
  public boolean hasErrors() {
    return diagnostics.stream()
        .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
  }
}
