package org.omg.CORBA;

/** Operation was invoked in an invalid ORB lifecycle state. */
public final class BAD_INV_ORDER extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates a BAD_INV_ORDER exception with default minor and completion status. */
  public BAD_INV_ORDER() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a BAD_INV_ORDER exception with a message. */
  public BAD_INV_ORDER(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a BAD_INV_ORDER exception with minor code and completion status. */
  public BAD_INV_ORDER(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates a BAD_INV_ORDER exception. */
  public BAD_INV_ORDER(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates a BAD_INV_ORDER exception with a cause. */
  public BAD_INV_ORDER(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
