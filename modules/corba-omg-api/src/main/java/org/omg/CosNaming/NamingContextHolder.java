package org.omg.CosNaming;

/** Holder for NamingContext references. */
@SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public final class NamingContextHolder implements org.omg.CORBA.portable.Streamable {

  /** Held value. */
  public NamingContext value;

  /** Creates an empty holder. */
  public NamingContextHolder() {}

  /** Creates a holder with a value. */
  public NamingContextHolder(NamingContext value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "NamingContextHolder[value=" + value + ']';
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
    return new org.omg.CORBA.NO_IMPLEMENT("CosNaming context holder streaming is not implemented");
  }
}
