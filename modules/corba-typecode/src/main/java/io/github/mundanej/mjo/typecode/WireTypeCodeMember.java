package io.github.mundanej.mjo.typecode;

import java.util.Objects;

/** Named member in a struct or exception wire TypeCode. */
public record WireTypeCodeMember(String name, WireTypeCode typeCode) {

  /** Creates a validated member. */
  public WireTypeCodeMember {
    name = requireNonBlank(name);
    Objects.requireNonNull(typeCode, "typeCode");
  }

  private static String requireNonBlank(String value) {
    Objects.requireNonNull(value, "name");
    if (value.isBlank()) {
      throw new IllegalArgumentException("member name must not be blank");
    }
    return value;
  }
}
