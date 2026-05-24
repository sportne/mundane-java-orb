package org.omg.CORBA;

/** Data conversion or code-set conversion failed. */
public final class DATA_CONVERSION extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates a DATA_CONVERSION exception with default minor and completion status. */
  public DATA_CONVERSION() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a DATA_CONVERSION exception with a message. */
  public DATA_CONVERSION(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a DATA_CONVERSION exception with minor code and completion status. */
  public DATA_CONVERSION(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates a DATA_CONVERSION exception. */
  public DATA_CONVERSION(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates a DATA_CONVERSION exception with a cause. */
  public DATA_CONVERSION(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
