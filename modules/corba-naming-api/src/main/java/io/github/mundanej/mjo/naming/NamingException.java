package io.github.mundanej.mjo.naming;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Thrown when local Naming Service validation or lookup fails. */
public final class NamingException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  private final String codeValue;

  /** Creates a naming exception with a stable diagnostic code. */
  public NamingException(DiagnosticCode code, String message) {
    super(requireNonBlank(message, "message"));
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
