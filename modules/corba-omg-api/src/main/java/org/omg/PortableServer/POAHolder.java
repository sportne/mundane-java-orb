package org.omg.PortableServer;

/** Holder for POA references. */
@SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public final class POAHolder implements org.omg.CORBA.portable.Streamable {

  /** Held value. */
  public POA value;

  /** Creates an empty holder. */
  public POAHolder() {}

  /** Creates a holder with a value. */
  public POAHolder(POA value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "POAHolder[value=" + value + ']';
  }

  @Override
  public void _read(org.omg.CORBA.portable.InputStream input) {
    throw unsupported();
  }

  @Override
  public void _write(org.omg.CORBA.portable.OutputStream output) {
    throw unsupported();
  }

  @Override
  public org.omg.CORBA.TypeCode _type() {
    throw unsupported();
  }

  private static org.omg.CORBA.NO_IMPLEMENT unsupported() {
    return new org.omg.CORBA.NO_IMPLEMENT("POA holder streaming is not implemented");
  }
}
