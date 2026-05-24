package org.omg.CosNaming.NamingContextPackage;

/** Naming operation cannot proceed in this context. */
@SuppressWarnings("serial")
public final class CannotProceed extends org.omg.CORBA.UserException {

  private static final long serialVersionUID = 1L;

  /** Context where resolution stopped. */
  public org.omg.CosNaming.NamingContext cxt;

  /** Remaining name. */
  public org.omg.CosNaming.NameComponent[] rest_of_name;

  /** Creates an empty exception. */
  public CannotProceed() {}

  /** Creates an exception. */
  public CannotProceed(
      org.omg.CosNaming.NamingContext context, org.omg.CosNaming.NameComponent[] restOfName) {
    this.cxt = context;
    this.rest_of_name = restOfName;
  }
}
