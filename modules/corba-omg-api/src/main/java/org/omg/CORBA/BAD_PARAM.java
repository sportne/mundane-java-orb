package org.omg.CORBA;

/** Parameter or argument validation failed. */
public final class BAD_PARAM extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Creates a BAD_PARAM exception. */
  public BAD_PARAM(String message, int minor, CompletionStatus completed) {
    super(message, minor, completed);
  }

  /** Creates a BAD_PARAM exception with a cause. */
  public BAD_PARAM(String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause, minor, completed);
  }
}
