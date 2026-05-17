package org.omg.CORBA;

import java.util.Objects;

/** Base class for CORBA system exceptions. */
public abstract class SystemException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Vendor minor code. */
  public final int minor;

  /** Operation completion status. */
  public final CompletionStatus completed;

  /** Creates a system exception with message, minor code, and completion status. */
  protected SystemException(String message, int minor, CompletionStatus completed) {
    super(message);
    this.minor = minor;
    this.completed = Objects.requireNonNull(completed, "completed");
  }

  /** Creates a system exception with message, cause, minor code, and completion status. */
  protected SystemException(
      String message, Throwable cause, int minor, CompletionStatus completed) {
    super(message, cause);
    this.minor = minor;
    this.completed = Objects.requireNonNull(completed, "completed");
  }
}
