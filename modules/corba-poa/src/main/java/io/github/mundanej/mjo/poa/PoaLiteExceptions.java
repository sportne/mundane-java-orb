package io.github.mundanej.mjo.poa;

import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.OBJECT_NOT_EXIST;

final class PoaLiteExceptions {

  private static final int POA_LITE_MINOR = 0;

  private PoaLiteExceptions() {}

  static <T> T requireNonNull(T value, String name) {
    if (value == null) {
      throw badParam(name + " must not be null");
    }
    return value;
  }

  static String requireNonBlank(String value, String name) {
    requireNonNull(value, name);
    if (value.isBlank()) {
      throw badParam(name + " must not be blank");
    }
    return value;
  }

  static BAD_PARAM badParam(String message) {
    return new BAD_PARAM(message, POA_LITE_MINOR, CompletionStatus.COMPLETED_NO);
  }

  static BAD_INV_ORDER badInvOrder(String message) {
    return new BAD_INV_ORDER(message, POA_LITE_MINOR, CompletionStatus.COMPLETED_NO);
  }

  static OBJECT_NOT_EXIST objectNotExist(String message) {
    return new OBJECT_NOT_EXIST(message, POA_LITE_MINOR, CompletionStatus.COMPLETED_NO);
  }
}
