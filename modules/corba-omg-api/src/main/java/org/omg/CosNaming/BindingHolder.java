package org.omg.CosNaming;

/** Holder for a CosNaming binding. */
@SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public final class BindingHolder implements org.omg.CORBA.portable.Streamable {

  /** Held value. */
  public Binding value;

  /** Creates an empty holder. */
  public BindingHolder() {}

  /** Creates a holder with a value. */
  public BindingHolder(Binding value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "BindingHolder[value=" + value + ']';
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
    return new org.omg.CORBA.NO_IMPLEMENT("CosNaming binding holder streaming is not implemented");
  }
}
