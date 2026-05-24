package org.omg.CORBA.TypeCodePackage;

/** TypeCode operation is invalid for the TypeCode kind. */
public final class BadKind extends org.omg.CORBA.UserException {

  private static final long serialVersionUID = 1L;

  /** Creates a bad-kind exception. */
  public BadKind() {
    super();
  }

  /** Creates a bad-kind exception with a message. */
  public BadKind(String message) {
    super(message);
  }
}
