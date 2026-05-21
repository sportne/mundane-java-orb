package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Runtime failure raised by the bounded local RMI-IIOP wire integration slice. */
public final class RmiIiopWireException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient DiagnosticCode code;

  /** Creates a wire exception with a stable RMI diagnostic code. */
  public RmiIiopWireException(DiagnosticCode code, String message) {
    super(requireNonBlank(message, "message"));
    this.code = Objects.requireNonNull(code, "code");
  }

  /** Creates a wire exception with a stable RMI diagnostic code and cause. */
  public RmiIiopWireException(DiagnosticCode code, String message, Throwable cause) {
    super(requireNonBlank(message, "message"), Objects.requireNonNull(cause, "cause"));
    this.code = Objects.requireNonNull(code, "code");
  }

  /** Returns the stable diagnostic code for this failure. */
  public DiagnosticCode code() {
    return code;
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
