package org.omg.CORBA;

import java.util.Objects;

/** Struct member metadata value. */
@SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public final class StructMember {

  public String name;
  public TypeCode type;
  public IDLType type_def;

  /** Creates an empty struct member. */
  public StructMember() {}

  /** Creates a struct member. */
  public StructMember(String name, TypeCode type, IDLType typeDefinition) {
    this.name = name;
    this.type = type;
    this.type_def = typeDefinition;
  }

  @Override
  public String toString() {
    return "StructMember["
        + "name="
        + name
        + ", type="
        + Objects.toString(type)
        + ", type_def="
        + Objects.toString(type_def)
        + ']';
  }
}
