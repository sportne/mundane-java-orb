package org.omg.CORBA.ORBPackage;

/** Initial reference name is invalid or unknown. */
public final class InvalidName extends org.omg.CORBA.UserException {

  private static final long serialVersionUID = 1L;

  /** Creates an invalid-name exception. */
  public InvalidName() {
    super();
  }

  /** Creates an invalid-name exception with a message. */
  public InvalidName(String message) {
    super(message);
  }
}
