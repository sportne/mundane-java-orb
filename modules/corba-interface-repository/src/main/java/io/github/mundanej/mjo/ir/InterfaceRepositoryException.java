package io.github.mundanej.mjo.ir;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Thrown when local static Interface Repository metadata is invalid or missing. */
public final class InterfaceRepositoryException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  private final String codeValue;

  /** Creates an Interface Repository exception with a stable diagnostic code. */
  public InterfaceRepositoryException(DiagnosticCode code, String message) {
    super(requireNonBlank(message, "message"));
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
