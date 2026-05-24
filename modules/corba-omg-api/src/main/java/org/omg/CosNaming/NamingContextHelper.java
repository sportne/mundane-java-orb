package org.omg.CosNaming;

/** Compile-safe helper for NamingContext references. */
public final class NamingContextHelper {

  private static final String ID = "IDL:omg.org/CosNaming/NamingContext:1.0";

  private NamingContextHelper() {}

  /** Returns the repository ID. */
  public static String id() {
    return ID;
  }

  /** Narrows an object reference to NamingContext. */
  public static NamingContext narrow(org.omg.CORBA.Object object) {
    return (NamingContext) object;
  }
}
