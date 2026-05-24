package org.omg.CORBA;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for the minimal CORBA exception API. */
@Tag("unit")
final class SystemExceptionTest {

  @Test
  void systemExceptionsPreserveMessageMinorCompletionAndCause() {
    IllegalArgumentException cause = new IllegalArgumentException("bad input");
    UNKNOWN exception = new UNKNOWN("unknown failure", cause, 7, CompletionStatus.COMPLETED_MAYBE);

    assertEquals("unknown failure", exception.getMessage());
    assertEquals(7, exception.minor);
    assertEquals(CompletionStatus.COMPLETED_MAYBE, exception.completed);
    assertSame(cause, exception.getCause());
  }

  @Test
  void systemExceptionsPreserveBoundaryMinorCodesAndNullMessages() {
    BAD_PARAM minimum = new BAD_PARAM(null, Integer.MIN_VALUE, CompletionStatus.COMPLETED_YES);
    BAD_PARAM maximum = new BAD_PARAM("max", Integer.MAX_VALUE, CompletionStatus.COMPLETED_NO);

    assertNull(minimum.getMessage());
    assertEquals(Integer.MIN_VALUE, minimum.minor);
    assertEquals(CompletionStatus.COMPLETED_YES, minimum.completed);
    assertEquals(Integer.MAX_VALUE, maximum.minor);
    assertEquals(CompletionStatus.COMPLETED_NO, maximum.completed);
  }

  @Test
  void concreteSystemExceptionsExposeLocalInvocationConstructors() {
    IllegalStateException cause = new IllegalStateException("cause");

    assertSystemException(new BAD_PARAM("bad parameter", 1, CompletionStatus.COMPLETED_NO));
    assertSystemException(
        new BAD_PARAM("bad parameter", cause, 2, CompletionStatus.COMPLETED_MAYBE), cause);
    assertSystemException(new BAD_OPERATION("bad op", 3, CompletionStatus.COMPLETED_NO));
    assertSystemException(
        new BAD_OPERATION("bad op", cause, 4, CompletionStatus.COMPLETED_MAYBE), cause);
    assertSystemException(new BAD_INV_ORDER("bad order", 5, CompletionStatus.COMPLETED_NO));
    assertSystemException(
        new BAD_INV_ORDER("bad order", cause, 6, CompletionStatus.COMPLETED_MAYBE), cause);
    assertSystemException(new OBJECT_NOT_EXIST("missing", 7, CompletionStatus.COMPLETED_NO));
    assertSystemException(
        new OBJECT_NOT_EXIST("missing", cause, 8, CompletionStatus.COMPLETED_MAYBE), cause);
    assertSystemException(new UNKNOWN("unknown", 9, CompletionStatus.COMPLETED_NO));
    assertSystemException(
        new UNKNOWN("unknown", cause, 10, CompletionStatus.COMPLETED_MAYBE), cause);
  }

  @Test
  void g10SystemExceptionsExposeLegacyConstructorSet() throws Exception {
    IllegalStateException cause = new IllegalStateException("cause");
    List<Class<? extends SystemException>> exceptionTypes =
        List.of(
            BAD_CONTEXT.class,
            BAD_INV_ORDER.class,
            BAD_OPERATION.class,
            BAD_PARAM.class,
            BAD_QOS.class,
            BAD_TYPECODE.class,
            COMM_FAILURE.class,
            DATA_CONVERSION.class,
            INITIALIZE.class,
            INTERNAL.class,
            INV_OBJREF.class,
            INV_POLICY.class,
            MARSHAL.class,
            NO_IMPLEMENT.class,
            NO_RESOURCES.class,
            OBJECT_NOT_EXIST.class,
            OBJ_ADAPTER.class,
            TRANSIENT.class,
            UNKNOWN.class);

    for (Class<? extends SystemException> exceptionType : exceptionTypes) {
      assertSystemException(exceptionType.getConstructor().newInstance());
      assertSystemException(exceptionType.getConstructor(String.class).newInstance("message"));
      assertSystemException(
          exceptionType
              .getConstructor(int.class, CompletionStatus.class)
              .newInstance(11, CompletionStatus.COMPLETED_NO));
      assertSystemException(
          exceptionType
              .getConstructor(String.class, int.class, CompletionStatus.class)
              .newInstance("message", 12, CompletionStatus.COMPLETED_YES));
      assertSystemException(
          exceptionType
              .getConstructor(String.class, Throwable.class, int.class, CompletionStatus.class)
              .newInstance("message", cause, 13, CompletionStatus.COMPLETED_MAYBE),
          cause);
      assertNullCompletionRejected(exceptionType);
    }
  }

  @Test
  void concreteSystemExceptionsRejectNullCompletionStatus() {
    assertThrows(
        NullPointerException.class,
        () -> {
          throw new BAD_PARAM("bad", 0, null);
        });
  }

  @Test
  void completionStatusOrderIsStable() {
    assertEquals(
        java.util.List.of(
            CompletionStatus.COMPLETED_YES,
            CompletionStatus.COMPLETED_NO,
            CompletionStatus.COMPLETED_MAYBE),
        java.util.List.of(CompletionStatus.values()));
  }

  @Test
  void userExceptionIsCheckedCompatibilityBase() {
    UserException exception = new UserException("declared");
    RuntimeException cause = new RuntimeException("cause");
    UserException withCause = new UserException("declared", cause);
    UserException empty = new UserException();

    assertInstanceOf(Exception.class, exception);
    assertEquals("declared", exception.getMessage());
    assertEquals("declared", withCause.getMessage());
    assertSame(cause, withCause.getCause());
    assertNull(empty.getMessage());
  }

  private static void assertSystemException(SystemException exception) {
    assertNull(exception.getCause());
    assertInstanceOf(SystemException.class, exception);
  }

  private static void assertSystemException(SystemException exception, Throwable cause) {
    assertSame(cause, exception.getCause());
    assertInstanceOf(SystemException.class, exception);
  }

  private static void assertNullCompletionRejected(Class<? extends SystemException> exceptionType)
      throws Exception {
    InvocationTargetException exception =
        assertThrows(
            InvocationTargetException.class,
            () ->
                exceptionType
                    .getConstructor(String.class, int.class, CompletionStatus.class)
                    .newInstance("message", 0, null));

    assertInstanceOf(NullPointerException.class, exception.getCause());
  }
}
