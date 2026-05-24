package org.omg.CORBA;

/** ORB or compatibility API initialization failed. */
public final class INITIALIZE extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates an INITIALIZE exception with default minor and completion status. */
  public INITIALIZE() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates an INITIALIZE exception with a message. */
  public INITIALIZE(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates an INITIALIZE exception with minor code and completion status. */
  public INITIALIZE(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates an INITIALIZE exception. */
  public INITIALIZE(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates an INITIALIZE exception with a cause. */
  public INITIALIZE(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
