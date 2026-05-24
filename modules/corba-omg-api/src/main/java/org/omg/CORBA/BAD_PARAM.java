package org.omg.CORBA;

/** Parameter or argument validation failed. */
public final class BAD_PARAM extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates a BAD_PARAM exception with default minor and completion status. */
  public BAD_PARAM() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a BAD_PARAM exception with a message. */
  public BAD_PARAM(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a BAD_PARAM exception with minor code and completion status. */
  public BAD_PARAM(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates a BAD_PARAM exception. */
  public BAD_PARAM(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates a BAD_PARAM exception with a cause. */
  public BAD_PARAM(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
