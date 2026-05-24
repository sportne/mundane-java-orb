package io.github.mundanej.mjo.interceptors;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Runtime exception raised by the local Portable Interceptor runtime. */
public final class InterceptorException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String codeValue;

  /** Creates a diagnostic interceptor exception. */
  public InterceptorException(DiagnosticCode code, String message) {
    super(requireNonBlank(message, "message"));
    this.codeValue = Objects.requireNonNull(code, "code").value();
  }

  /** Creates a diagnostic interceptor exception with a cause. */
  public InterceptorException(DiagnosticCode code, String message, Throwable cause) {
    super(requireNonBlank(message, "message"), Objects.requireNonNull(cause, "cause"));
    this.codeValue = Objects.requireNonNull(code, "code").value();
  }

  /** Returns the stable diagnostic code. */
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
