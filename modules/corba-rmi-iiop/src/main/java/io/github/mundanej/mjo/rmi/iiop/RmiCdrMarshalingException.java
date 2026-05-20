package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Thrown when local RMI-IIOP CDR marshaling violates the approved G7-060 slice. */
public final class RmiCdrMarshalingException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  private final String codeValue;

  /** Creates an RMI CDR marshaling exception with a stable diagnostic code. */
  public RmiCdrMarshalingException(DiagnosticCode code, String message) {
    super(requireNonBlank(message, "message"));
    this.codeValue = Objects.requireNonNull(code, "code").value();
  }

  /** Returns the stable diagnostic code for this marshaling failure. */
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
