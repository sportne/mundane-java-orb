package io.github.mundanej.mjo.iiop;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Thrown when local IIOP TCP transport setup or message exchange fails. */
public final class IiopException extends IllegalStateException {

  private static final long serialVersionUID = 1L;

  private final String codeValue;

  /** Creates an IIOP exception with a stable diagnostic code. */
  public IiopException(DiagnosticCode code, String message) {
    super(requireNonBlank(message, "message"));
    this.codeValue = Objects.requireNonNull(code, "code").value();
  }

  /** Creates an IIOP exception with a stable diagnostic code and cause. */
  public IiopException(DiagnosticCode code, String message, Throwable cause) {
    super(requireNonBlank(message, "message"), Objects.requireNonNull(cause, "cause"));
    this.codeValue = Objects.requireNonNull(code, "code").value();
  }

  /** Returns the stable diagnostic code for this IIOP failure. */
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
