package org.omg.CosNaming;

/** Compile-safe helper for NamingContextExt references. */
public final class NamingContextExtHelper {

  private static final String ID = "IDL:omg.org/CosNaming/NamingContextExt:1.0";

  private NamingContextExtHelper() {}

  /** Returns the repository ID. */
  public static String id() {
    return ID;
  }

  /** Narrows an object reference to NamingContextExt. */
  public static NamingContextExt narrow(org.omg.CORBA.Object object) {
    return (NamingContextExt) object;
  }
}
