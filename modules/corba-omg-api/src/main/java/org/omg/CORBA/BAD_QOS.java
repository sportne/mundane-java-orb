package org.omg.CORBA;

/** Quality-of-service policy negotiation failed. */
public final class BAD_QOS extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates a BAD_QOS exception with default minor and completion status. */
  public BAD_QOS() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a BAD_QOS exception with a message. */
  public BAD_QOS(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a BAD_QOS exception with minor code and completion status. */
  public BAD_QOS(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates a BAD_QOS exception. */
  public BAD_QOS(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates a BAD_QOS exception with a cause. */
  public BAD_QOS(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
