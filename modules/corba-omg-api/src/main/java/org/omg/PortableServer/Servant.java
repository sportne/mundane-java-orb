package org.omg.PortableServer;

/** Base class for generated POA servants. */
public abstract class Servant implements org.omg.CORBA.portable.InvokeHandler {

  /** Creates a servant. */
  protected Servant() {}

  /** Returns repository IDs for this servant. */
  public abstract String[] _all_interfaces(POA poa, byte[] objectId);

  /** Returns this servant as a CORBA object. */
  public org.omg.CORBA.Object _this_object() {
    throw unsupported();
  }

  /** Returns this servant as a CORBA object associated with an ORB. */
  public org.omg.CORBA.Object _this_object(org.omg.CORBA.ORB orb) {
    throw unsupported();
  }

  /** Returns the default POA for this servant. */
  public POA _default_POA() {
    throw unsupported();
  }

  /** Returns the active POA for this servant. */
  public POA _poa() {
    throw unsupported();
  }

  /** Returns the active object ID for this servant. */
  public byte[] _object_id() {
    throw unsupported();
  }

  private static org.omg.CORBA.NO_IMPLEMENT unsupported() {
    return new org.omg.CORBA.NO_IMPLEMENT("PortableServer servant behavior is not implemented");
  }
}
