package io.github.mundanej.mjo.time;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Thrown when the supported Time Service subset rejects input or clock state. */
public final class TimeServiceException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String codeValue;

  /** Creates a Time Service exception with a stable diagnostic code. */
  public TimeServiceException(DiagnosticCode code, String message) {
    super(requireNonBlank(message, "message"));
    codeValue = Objects.requireNonNull(code, "code").value();
  }

  /** Creates a Time Service exception with a stable diagnostic code and cause. */
  public TimeServiceException(DiagnosticCode code, String message, Throwable cause) {
    super(requireNonBlank(message, "message"), Objects.requireNonNull(cause, "cause"));
    codeValue = Objects.requireNonNull(code, "code").value();
  }

  /** Returns the stable diagnostic code for this failure. */
  public DiagnosticCode code() {
    return new DiagnosticCode(codeValue);
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
