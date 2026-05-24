package org.omg.CORBA;

/** A transient condition prevented an invocation from completing. */
public final class TRANSIENT extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates a TRANSIENT exception with default minor and completion status. */
  public TRANSIENT() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a TRANSIENT exception with a message. */
  public TRANSIENT(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a TRANSIENT exception with minor code and completion status. */
  public TRANSIENT(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates a TRANSIENT exception. */
  public TRANSIENT(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates a TRANSIENT exception with a cause. */
  public TRANSIENT(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
