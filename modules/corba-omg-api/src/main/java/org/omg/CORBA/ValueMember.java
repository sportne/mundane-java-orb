package org.omg.CORBA;

import java.util.Objects;

/** Value member metadata value. */
@SuppressWarnings("UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD")
public final class ValueMember {

  public String name;
  public String id;
  public String defined_in;
  public String version;
  public TypeCode type;
  public IDLType type_def;
  public short access;

  /** Creates an empty value member. */
  public ValueMember() {}

  @Override
  public String toString() {
    return "ValueMember["
        + "name="
        + name
        + ", id="
        + id
        + ", defined_in="
        + defined_in
        + ", version="
        + version
        + ", type="
        + Objects.toString(type)
        + ", type_def="
        + Objects.toString(type_def)
        + ", access="
        + access
        + ']';
  }
}
