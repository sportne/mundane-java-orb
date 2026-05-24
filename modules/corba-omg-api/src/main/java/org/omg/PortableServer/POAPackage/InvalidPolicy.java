package org.omg.PortableServer.POAPackage;

/** A supplied policy list is invalid. */
public final class InvalidPolicy extends org.omg.CORBA.UserException {
  private static final long serialVersionUID = 1L;

  /** Index of the invalid policy. */
  public short index;

  /** Creates the exception. */
  public InvalidPolicy() {}

  /** Creates the exception with an invalid-policy index. */
  public InvalidPolicy(short index) {
    this.index = index;
  }
}
