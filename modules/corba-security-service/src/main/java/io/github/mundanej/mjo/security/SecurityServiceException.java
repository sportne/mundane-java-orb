package io.github.mundanej.mjo.security;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Thrown when the supported Security Service subset rejects input or state. */
@SuppressWarnings("serial")
public final class SecurityServiceException extends RuntimeException {

  private final String codeValue;

  /** Creates a Security Service exception with a stable diagnostic code. */
  public SecurityServiceException(DiagnosticCode code, String message) {
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
