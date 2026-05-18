package io.github.mundanej.mjo.dynamic;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Thrown when local descriptor-backed dynamic behavior fails deterministically. */
public class DynamicException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String codeValue;

  /** Creates a dynamic exception with a stable diagnostic code. */
  public DynamicException(DiagnosticCode code, String message) {
    super(requireNonBlank(message, "message"));
    this.codeValue = Objects.requireNonNull(code, "code").value();
  }

  /** Creates a dynamic exception with a stable diagnostic code and cause. */
  public DynamicException(DiagnosticCode code, String message, Throwable cause) {
    super(requireNonBlank(message, "message"), Objects.requireNonNull(cause, "cause"));
    this.codeValue = Objects.requireNonNull(code, "code").value();
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
