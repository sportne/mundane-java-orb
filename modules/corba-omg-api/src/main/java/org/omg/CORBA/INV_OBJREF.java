package org.omg.CORBA;

/** Object reference syntax or contents are invalid. */
public final class INV_OBJREF extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates an INV_OBJREF exception with default minor and completion status. */
  public INV_OBJREF() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates an INV_OBJREF exception with a message. */
  public INV_OBJREF(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates an INV_OBJREF exception with minor code and completion status. */
  public INV_OBJREF(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates an INV_OBJREF exception. */
  public INV_OBJREF(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates an INV_OBJREF exception with a cause. */
  public INV_OBJREF(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
