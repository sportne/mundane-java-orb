package org.omg.CORBA;

/** Base class for checked IDL user exceptions in the legacy compatibility API. */
public class UserException extends Exception {

  private static final long serialVersionUID = 1L;

  /** Creates a user exception with no detail message. */
  public UserException() {
    super();
  }

  /** Creates a user exception with a detail message. */
  public UserException(String message) {
    super(message);
  }

  /** Creates a user exception with a detail message and cause. */
  public UserException(String message, Throwable cause) {
    super(message, cause);
  }
}
