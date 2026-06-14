package io.github.mundanej.mjo.trading;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Thrown when the supported Trading Service subset rejects input or state. */
public final class TradingServiceException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String codeValue;

  /** Creates a Trading Service exception with a stable diagnostic code. */
  public TradingServiceException(DiagnosticCode code, String message) {
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
