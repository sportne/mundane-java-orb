package org.omg.PortableServer;

/** Holder for POA manager references. */
@SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public final class POAManagerHolder implements org.omg.CORBA.portable.Streamable {

  /** Held value. */
  public POAManager value;

  /** Creates an empty holder. */
  public POAManagerHolder() {}

  /** Creates a holder with a value. */
  public POAManagerHolder(POAManager value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "POAManagerHolder[value=" + value + ']';
  }

  @Override
  public void _read(org.omg.CORBA.portable.InputStream input) {
    throw new org.omg.CORBA.NO_IMPLEMENT("POAManager holder streaming is not implemented");
  }

  @Override
  public void _write(org.omg.CORBA.portable.OutputStream output) {
    throw new org.omg.CORBA.NO_IMPLEMENT("POAManager holder streaming is not implemented");
  }

  @Override
  public org.omg.CORBA.TypeCode _type() {
    throw new org.omg.CORBA.NO_IMPLEMENT("POAManager holder streaming is not implemented");
  }
}
