package io.github.mundanej.mjo.common;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable diagnostic value.
 *
 * @param code stable diagnostic code
 * @param severity diagnostic severity
 * @param message human-readable diagnostic message
 * @param span optional source span for source-bound diagnostics
 */
public record Diagnostic(
    DiagnosticCode code, DiagnosticSeverity severity, String message, Optional<SourceSpan> span) {

  /** Creates a validated diagnostic. */
  public Diagnostic {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(severity, "severity");
    message = requireNonBlank(message, "message");
    Objects.requireNonNull(span, "span");
  }

  /** Creates a diagnostic that is not tied to a source span. */
  public static Diagnostic withoutSpan(
      DiagnosticCode code, DiagnosticSeverity severity, String message) {
    return new Diagnostic(code, severity, message, Optional.empty());
  }

  /** Creates a diagnostic tied to a source span. */
  public static Diagnostic withSpan(
      DiagnosticCode code, DiagnosticSeverity severity, String message, SourceSpan span) {
    return new Diagnostic(code, severity, message, Optional.of(span));
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
