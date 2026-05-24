package org.omg.CORBA;

/** Communication with a remote peer failed. */
public final class COMM_FAILURE extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates a COMM_FAILURE exception with default minor and completion status. */
  public COMM_FAILURE() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a COMM_FAILURE exception with a message. */
  public COMM_FAILURE(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a COMM_FAILURE exception with minor code and completion status. */
  public COMM_FAILURE(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates a COMM_FAILURE exception. */
  public COMM_FAILURE(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates a COMM_FAILURE exception with a cause. */
  public COMM_FAILURE(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
