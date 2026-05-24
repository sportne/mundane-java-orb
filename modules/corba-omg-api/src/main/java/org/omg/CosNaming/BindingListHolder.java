package org.omg.CosNaming;

import java.util.Arrays;

/** Holder for CosNaming binding lists. */
@SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public final class BindingListHolder implements org.omg.CORBA.portable.Streamable {

  /** Held value. */
  public Binding[] value;

  /** Creates an empty holder. */
  public BindingListHolder() {}

  /** Creates a holder with a value. */
  public BindingListHolder(Binding[] value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "BindingListHolder[value=" + Arrays.toString(value) + ']';
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
    return new org.omg.CORBA.NO_IMPLEMENT("CosNaming binding list streaming is not implemented");
  }
}
