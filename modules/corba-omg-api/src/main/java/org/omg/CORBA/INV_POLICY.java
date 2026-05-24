package org.omg.CORBA;

/** Policy value or policy combination is invalid. */
public final class INV_POLICY extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates an INV_POLICY exception with default minor and completion status. */
  public INV_POLICY() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates an INV_POLICY exception with a message. */
  public INV_POLICY(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates an INV_POLICY exception with minor code and completion status. */
  public INV_POLICY(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates an INV_POLICY exception. */
  public INV_POLICY(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates an INV_POLICY exception with a cause. */
  public INV_POLICY(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
