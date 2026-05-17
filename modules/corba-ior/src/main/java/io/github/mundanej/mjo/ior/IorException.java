package io.github.mundanej.mjo.ior;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Thrown when IOR or object URL input violates the supported rules. */
public final class IorException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  private final String codeValue;

  /** Creates an IOR exception with a stable diagnostic code. */
  public IorException(DiagnosticCode code, String message) {
    super(requireNonBlank(message, "message"));
    this.codeValue = Objects.requireNonNull(code, "code").value();
  }

  /** Returns the stable diagnostic code for this IOR failure. */
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
