package io.github.mundanej.mjo.orb;

import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.Objects;
import java.util.Optional;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.UNKNOWN;

/** Maps local invocation failures to the minimal CORBA exception surface. */
final class LocalExceptionMapper {

  private static final int LOCAL_MINOR = 0;

  private LocalExceptionMapper() {}

  static <T> T requireNonNull(T value, String name) {
    if (value == null) {
      throw badParam(name + " must not be null");
    }
    return value;
  }

  static BAD_PARAM badParam(String message) {
    return new BAD_PARAM(message, LOCAL_MINOR, CompletionStatus.COMPLETED_NO);
  }

  static BAD_OPERATION badOperation(String message) {
    return new BAD_OPERATION(message, LOCAL_MINOR, CompletionStatus.COMPLETED_NO);
  }

  static BAD_INV_ORDER badInvOrder(String message) {
    return new BAD_INV_ORDER(message, LOCAL_MINOR, CompletionStatus.COMPLETED_NO);
  }

  static OBJECT_NOT_EXIST objectNotExist(String message) {
    return new OBJECT_NOT_EXIST(message, LOCAL_MINOR, CompletionStatus.COMPLETED_NO);
  }

  static RuntimeException mapDispatcherException(
      IdlOperationDescriptor operation, Exception exception) {
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(exception, "exception");
    Optional<IdlTypeReference> raisedType = declaredUserException(operation, exception);
    if (raisedType.isPresent()) {
      return new LocalInvocationUserException(operation, raisedType.orElseThrow(), exception);
    }
    return new UNKNOWN(
        "Undeclared local invocation exception from operation " + operation.name(),
        exception,
        LOCAL_MINOR,
        CompletionStatus.COMPLETED_MAYBE);
  }

  private static Optional<IdlTypeReference> declaredUserException(
      IdlOperationDescriptor operation, Exception exception) {
    if (exception instanceof RuntimeException) {
      return Optional.empty();
    }
    String exceptionClassName = exception.getClass().getName();
    return operation.raises().stream()
        .filter(raisedType -> exceptionClassName.equals(raisedType.javaName()))
        .findFirst();
  }
}
