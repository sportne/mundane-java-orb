package io.github.mundanej.mjo.dynamic;

import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.Objects;

/** Wraps a declared user exception raised through local dynamic invocation. */
public final class DynamicUserException extends DynamicException {

  private static final long serialVersionUID = 1L;

  private final transient IdlOperationDescriptor operation;
  private final transient IdlTypeReference raisedType;

  /** Creates a dynamic user-exception wrapper. */
  public DynamicUserException(
      IdlOperationDescriptor operation, IdlTypeReference raisedType, Exception userException) {
    super(
        DynamicDiagnosticCodes.USER_EXCEPTION,
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

  /** Returns the declared exception type reference. */
  public IdlTypeReference raisedType() {
    return raisedType;
  }

  /** Returns the original generated checked user exception instance. */
  public Exception userException() {
    return (Exception) getCause();
  }
}
