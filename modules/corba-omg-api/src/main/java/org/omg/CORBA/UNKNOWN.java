package org.omg.CORBA;

/** Unclassified local or remote invocation failure. */
public final class UNKNOWN extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates an UNKNOWN exception with default minor and completion status. */
  public UNKNOWN() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates an UNKNOWN exception with a message. */
  public UNKNOWN(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates an UNKNOWN exception with minor code and completion status. */
  public UNKNOWN(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates an UNKNOWN exception. */
  public UNKNOWN(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates an UNKNOWN exception with a cause. */
  public UNKNOWN(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
