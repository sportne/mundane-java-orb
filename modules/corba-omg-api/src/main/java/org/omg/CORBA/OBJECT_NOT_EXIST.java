package org.omg.CORBA;

/** Target object reference does not name a known live object. */
public final class OBJECT_NOT_EXIST extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates an OBJECT_NOT_EXIST exception. */
  public OBJECT_NOT_EXIST(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates an OBJECT_NOT_EXIST exception with a cause. */
  public OBJECT_NOT_EXIST(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
