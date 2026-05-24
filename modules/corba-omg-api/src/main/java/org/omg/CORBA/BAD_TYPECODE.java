package org.omg.CORBA;

/** TypeCode construction or access failed. */
public final class BAD_TYPECODE extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates a BAD_TYPECODE exception with default minor and completion status. */
  public BAD_TYPECODE() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a BAD_TYPECODE exception with a message. */
  public BAD_TYPECODE(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a BAD_TYPECODE exception with minor code and completion status. */
  public BAD_TYPECODE(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates a BAD_TYPECODE exception. */
  public BAD_TYPECODE(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates a BAD_TYPECODE exception with a cause. */
  public BAD_TYPECODE(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
