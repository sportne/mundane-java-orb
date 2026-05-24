package org.omg.PortableServer;

/** Compile-safe helper for POA references. */
public final class POAHelper {

  private static final String ID = "IDL:omg.org/PortableServer/POA:2.3";

  private POAHelper() {}

  /** Returns the repository ID. */
  public static String id() {
    return ID;
  }

  /** Narrows an object reference to POA. */
  public static POA narrow(org.omg.CORBA.Object object) {
    return (POA) object;
  }
}
