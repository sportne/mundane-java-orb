package io.github.mundanej.mjo.event;

import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;

final class EventServiceCorbaExceptions {

  private EventServiceCorbaExceptions() {}

  static BAD_PARAM badParam(String message) {
    return new BAD_PARAM(message, 0, CompletionStatus.COMPLETED_NO);
  }

  static BAD_PARAM badParam(String message, Throwable cause) {
    return new BAD_PARAM(message, cause, 0, CompletionStatus.COMPLETED_NO);
  }

  static BAD_OPERATION badOperation(String message) {
    return new BAD_OPERATION(message, 0, CompletionStatus.COMPLETED_NO);
  }

  static RuntimeException from(EventServiceException exception) {
    return new BAD_PARAM(
        exception.getMessage(), exception, minor(exception), CompletionStatus.COMPLETED_NO);
  }

  private static int minor(EventServiceException exception) {
    return Integer.parseInt(exception.code().value().substring("EVNT-".length()));
  }
}
