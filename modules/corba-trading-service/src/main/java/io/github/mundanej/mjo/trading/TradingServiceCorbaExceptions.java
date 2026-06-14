package io.github.mundanej.mjo.trading;

import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;

final class TradingServiceCorbaExceptions {

  private TradingServiceCorbaExceptions() {}

  static BAD_PARAM badParam(String message) {
    return new BAD_PARAM(message, 0, CompletionStatus.COMPLETED_NO);
  }

  static BAD_PARAM badParam(String message, Throwable cause) {
    return new BAD_PARAM(message, cause, 0, CompletionStatus.COMPLETED_NO);
  }

  static BAD_OPERATION badOperation(String message) {
    return new BAD_OPERATION(message, 0, CompletionStatus.COMPLETED_NO);
  }

  static RuntimeException from(TradingServiceException exception) {
    return new BAD_PARAM(
        exception.getMessage(), exception, minor(exception), CompletionStatus.COMPLETED_NO);
  }

  private static int minor(TradingServiceException exception) {
    return Integer.parseInt(exception.code().value().substring("TRAD-".length()));
  }
}
