package org.omg.CORBA;

/** Context use failed. */
public final class BAD_CONTEXT extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates a BAD_CONTEXT exception with default minor and completion status. */
  public BAD_CONTEXT() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a BAD_CONTEXT exception with a message. */
  public BAD_CONTEXT(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a BAD_CONTEXT exception with minor code and completion status. */
  public BAD_CONTEXT(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates a BAD_CONTEXT exception. */
  public BAD_CONTEXT(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates a BAD_CONTEXT exception with a cause. */
  public BAD_CONTEXT(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
