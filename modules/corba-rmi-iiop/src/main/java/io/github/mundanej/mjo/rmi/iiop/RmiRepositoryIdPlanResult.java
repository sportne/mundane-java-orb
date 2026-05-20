package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result of RMI repository ID planning.
 *
 * @param plan repository ID plan when no error diagnostics were emitted
 * @param diagnostics planning diagnostics in deterministic order
 */
public record RmiRepositoryIdPlanResult(
    Optional<RmiRepositoryIdPlan> plan, List<Diagnostic> diagnostics) {

  /** Creates an immutable planning result. */
  public RmiRepositoryIdPlanResult {
    Objects.requireNonNull(plan, "plan");
    diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    if (diagnostics.stream()
            .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR)
        && plan.isPresent()) {
      throw new IllegalArgumentException("plan must be empty when errors are present");
    }
  }

  /** Returns whether any emitted diagnostic is an error. */
  public boolean hasErrors() {
    return diagnostics.stream()
        .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
  }
}
