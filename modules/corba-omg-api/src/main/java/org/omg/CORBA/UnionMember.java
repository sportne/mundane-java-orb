package org.omg.CORBA;

import java.util.Objects;

/** Union member metadata value. */
@SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public final class UnionMember {

  public String name;
  public Any label;
  public TypeCode type;
  public IDLType type_def;

  /** Creates an empty union member. */
  public UnionMember() {}

  /** Creates a union member. */
  public UnionMember(String name, Any label, TypeCode type, IDLType typeDefinition) {
    this.name = name;
    this.label = label;
    this.type = type;
    this.type_def = typeDefinition;
  }

  @Override
  public String toString() {
    return "UnionMember["
        + "name="
        + name
        + ", label="
        + Objects.toString(label)
        + ", type="
        + Objects.toString(type)
        + ", type_def="
        + Objects.toString(type_def)
        + ']';
  }
}
