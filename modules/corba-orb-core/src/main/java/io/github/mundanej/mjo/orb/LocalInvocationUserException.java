package io.github.mundanej.mjo.orb;

import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.Objects;

/** Wraps a declared IDL user exception raised by a local generated-style dispatcher. */
public final class LocalInvocationUserException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient IdlOperationDescriptor operation;
  private final transient IdlTypeReference raisedType;

  /** Creates a local user-exception wrapper. */
  public LocalInvocationUserException(
      IdlOperationDescriptor operation, IdlTypeReference raisedType, Exception userException) {
    super(
        "Declared user exception "
            + Objects.requireNonNull(raisedType, "raisedType").idlName()
            + " from operation "
            + Objects.requireNonNull(operation, "operation").name(),
        Objects.requireNonNull(userException, "userException"));
    this.operation = operation;
    this.raisedType = raisedType;
  }

  /** Returns the operation that raised the user exception. */
  public IdlOperationDescriptor operation() {
    return operation;
  }

  /** Returns the matching static raised-exception descriptor. */
  public IdlTypeReference raisedType() {
    return raisedType;
  }

  /** Returns the original generated checked user exception instance. */
  public Exception userException() {
    return (Exception) getCause();
  }
}
