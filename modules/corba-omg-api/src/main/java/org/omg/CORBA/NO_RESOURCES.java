package org.omg.CORBA;

/** Required local or remote resources are unavailable. */
public final class NO_RESOURCES extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates a NO_RESOURCES exception with default minor and completion status. */
  public NO_RESOURCES() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a NO_RESOURCES exception with a message. */
  public NO_RESOURCES(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a NO_RESOURCES exception with minor code and completion status. */
  public NO_RESOURCES(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates a NO_RESOURCES exception. */
  public NO_RESOURCES(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates a NO_RESOURCES exception with a cause. */
  public NO_RESOURCES(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
