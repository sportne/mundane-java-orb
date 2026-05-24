package org.omg.CORBA;

/** Object adapter processing failed. */
public final class OBJ_ADAPTER extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates an OBJ_ADAPTER exception with default minor and completion status. */
  public OBJ_ADAPTER() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates an OBJ_ADAPTER exception with a message. */
  public OBJ_ADAPTER(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates an OBJ_ADAPTER exception with minor code and completion status. */
  public OBJ_ADAPTER(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates an OBJ_ADAPTER exception. */
  public OBJ_ADAPTER(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates an OBJ_ADAPTER exception with a cause. */
  public OBJ_ADAPTER(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
