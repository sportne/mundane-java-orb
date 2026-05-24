package org.omg.CORBA;

/** Internal ORB processing failed. */
public final class INTERNAL extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates an INTERNAL exception with default minor and completion status. */
  public INTERNAL() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates an INTERNAL exception with a message. */
  public INTERNAL(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates an INTERNAL exception with minor code and completion status. */
  public INTERNAL(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates an INTERNAL exception. */
  public INTERNAL(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates an INTERNAL exception with a cause. */
  public INTERNAL(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
