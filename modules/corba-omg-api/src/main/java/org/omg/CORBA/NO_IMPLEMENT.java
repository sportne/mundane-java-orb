package org.omg.CORBA;

/** Requested compatibility API behavior is outside the implemented local slice. */
public final class NO_IMPLEMENT extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates a NO_IMPLEMENT exception with default minor and completion status. */
  public NO_IMPLEMENT() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a NO_IMPLEMENT exception with a message. */
  public NO_IMPLEMENT(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a NO_IMPLEMENT exception with minor code and completion status. */
  public NO_IMPLEMENT(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates a NO_IMPLEMENT exception. */
  public NO_IMPLEMENT(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates a NO_IMPLEMENT exception with a cause. */
  public NO_IMPLEMENT(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
