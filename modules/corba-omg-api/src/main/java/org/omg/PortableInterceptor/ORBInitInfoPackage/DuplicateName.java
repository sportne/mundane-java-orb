package org.omg.PortableInterceptor.ORBInitInfoPackage;

/** Interceptor name is already registered. */
public final class DuplicateName extends org.omg.CORBA.UserException {

  private static final long serialVersionUID = 1L;

  /** Duplicate name. */
  public String name;

  /** Creates an empty duplicate-name exception. */
  public DuplicateName() {}

  /** Creates a duplicate-name exception. */
  public DuplicateName(String name) {
    this.name = name;
  }
}
