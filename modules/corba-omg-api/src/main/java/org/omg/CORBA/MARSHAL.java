package org.omg.CORBA;

/** Marshaling or unmarshaling failed. */
public final class MARSHAL extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates a MARSHAL exception with default minor and completion status. */
  public MARSHAL() {
    this("", 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a MARSHAL exception with a message. */
  public MARSHAL(String message) {
    this(message, 0, CompletionStatus.COMPLETED_NO);
  }

  /** Creates a MARSHAL exception with minor code and completion status. */
  public MARSHAL(int minor, CompletionStatus completed) {
    this("", minor, completed);
  }

  /** Creates a MARSHAL exception. */
  public MARSHAL(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates a MARSHAL exception with a cause. */
  public MARSHAL(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
