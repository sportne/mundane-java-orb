package io.github.mundanej.mjo.typecode;

import java.util.Objects;

/**
 * One named member in a struct or exception TypeCode.
 *
 * @param name IDL member name
 * @param type member TypeCode
 */
public record IdlTypeCodeMember(String name, IdlTypeCode type) {

  /** Creates a validated TypeCode member. */
  public IdlTypeCodeMember {
    name = requireNonBlank(name, "name");
    Objects.requireNonNull(type, "type");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
