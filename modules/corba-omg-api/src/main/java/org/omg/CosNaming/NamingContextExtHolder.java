package org.omg.CosNaming;

/** Holder for NamingContextExt references. */
@SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public final class NamingContextExtHolder implements org.omg.CORBA.portable.Streamable {

  /** Held value. */
  public NamingContextExt value;

  /** Creates an empty holder. */
  public NamingContextExtHolder() {}

  /** Creates a holder with a value. */
  public NamingContextExtHolder(NamingContextExt value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "NamingContextExtHolder[value=" + value + ']';
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
    return new org.omg.CORBA.NO_IMPLEMENT("CosNaming ext holder streaming is not implemented");
  }
}
