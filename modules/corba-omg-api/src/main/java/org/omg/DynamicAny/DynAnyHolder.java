package org.omg.DynamicAny;

/** Holder for DynAny values. */
@SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public final class DynAnyHolder implements org.omg.CORBA.portable.Streamable {

  /** Held value. */
  public DynAny value;

  /** Creates an empty holder. */
  public DynAnyHolder() {}

  /** Creates a holder with a value. */
  public DynAnyHolder(DynAny value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "DynAnyHolder[value=" + value + ']';
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
    return new org.omg.CORBA.NO_IMPLEMENT("DynAny holder streaming is not implemented");
  }
}
