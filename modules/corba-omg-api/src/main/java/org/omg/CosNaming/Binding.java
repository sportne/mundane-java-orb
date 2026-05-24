package org.omg.CosNaming;

import java.util.Arrays;

/** CosNaming binding value. */
@SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public final class Binding implements org.omg.CORBA.portable.IDLEntity {

  /** Bound name. */
  public NameComponent[] binding_name;

  /** Binding type. */
  public BindingType binding_type;

  /** Creates an empty binding. */
  public Binding() {}

  /** Creates a binding. */
  public Binding(NameComponent[] bindingName, BindingType bindingType) {
    this.binding_name = bindingName;
    this.binding_type = bindingType;
  }

  @Override
  public String toString() {
    return "Binding["
        + "binding_name="
        + Arrays.toString(binding_name)
        + ", binding_type="
        + bindingTypeValue()
        + ']';
  }

  private String bindingTypeValue() {
    return binding_type == null ? "null" : Integer.toString(binding_type.value());
  }
}
