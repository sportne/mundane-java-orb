package io.github.mundanej.mjo.any;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Thrown when local Any values do not match their static TypeCode contract. */
public final class AnyException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  private final String codeValue;

  /** Creates an Any exception with a stable diagnostic code. */
  public AnyException(DiagnosticCode code, String message) {
    super(requireNonBlank(message, "message"));
    this.codeValue = Objects.requireNonNull(code, "code").value();
  }

  /** Returns the stable diagnostic code for this Any failure. */
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
