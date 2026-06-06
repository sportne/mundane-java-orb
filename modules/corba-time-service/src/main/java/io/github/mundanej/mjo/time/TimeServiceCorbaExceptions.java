package io.github.mundanej.mjo.time;

import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.UNKNOWN;

final class TimeServiceCorbaExceptions {

  private TimeServiceCorbaExceptions() {}

  static BAD_PARAM badParam(String message) {
    return new BAD_PARAM(message, 0, CompletionStatus.COMPLETED_NO);
  }

  static BAD_PARAM badParam(String message, Throwable cause) {
    return new BAD_PARAM(message, cause, 0, CompletionStatus.COMPLETED_NO);
  }

  static BAD_OPERATION badOperation(String message) {
    return new BAD_OPERATION(message, 0, CompletionStatus.COMPLETED_NO);
  }

  static RuntimeException from(TimeServiceException exception) {
    if (exception.code().equals(TimeServiceDiagnosticCodes.CLOCK_UNAVAILABLE)) {
      return new UNKNOWN(exception.getMessage(), exception, 5, CompletionStatus.COMPLETED_MAYBE);
    }
    return new BAD_PARAM(
        exception.getMessage(), exception, minor(exception), CompletionStatus.COMPLETED_NO);
  }

  private static int minor(TimeServiceException exception) {
    return Integer.parseInt(exception.code().value().substring("TIME-".length()));
  }
}
