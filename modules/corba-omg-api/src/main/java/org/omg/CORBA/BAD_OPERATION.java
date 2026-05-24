package org.omg.CORBA;

/** Requested operation is not valid for the target object. */
public final class BAD_OPERATION extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates a BAD_OPERATION exception with default minor and completion status. */
  public BAD_OPERATION() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a BAD_OPERATION exception with a message. */
  public BAD_OPERATION(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a BAD_OPERATION exception with minor code and completion status. */
  public BAD_OPERATION(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates a BAD_OPERATION exception. */
  public BAD_OPERATION(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates a BAD_OPERATION exception with a cause. */
  public BAD_OPERATION(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
