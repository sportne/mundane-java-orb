package org.omg.CosNaming.NamingContextPackage;

/** Name was not found. */
@SuppressWarnings("serial")
public final class NotFound extends org.omg.CORBA.UserException {

  private static final long serialVersionUID = 1L;

  /** Not-found reason. */
  public NotFoundReason why;

  /** Remaining name. */
  public org.omg.CosNaming.NameComponent[] rest_of_name;

  /** Creates an empty exception. */
  public NotFound() {}

  /** Creates an exception. */
  public NotFound(NotFoundReason why, org.omg.CosNaming.NameComponent[] restOfName) {
    this.why = why;
    this.rest_of_name = restOfName;
  }
}
