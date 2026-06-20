package io.github.mundanej.mjo.transaction;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Thrown when the supported Transaction Service subset rejects input or state. */
@SuppressWarnings("serial")
public final class TransactionServiceException extends RuntimeException {

  private final String codeValue;

  /** Creates a Transaction Service exception with a stable diagnostic code. */
  public TransactionServiceException(DiagnosticCode code, String message) {
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
