package io.github.mundanej.mjo.typecode;

import java.util.Objects;
import java.util.OptionalLong;

/** Union member in a wire TypeCode, with an optional numeric discriminator label. */
public record WireTypeCodeUnionMember(String name, OptionalLong label, WireTypeCode typeCode) {

  /** Creates a validated union member. */
  public WireTypeCodeUnionMember {
    name = requireNonBlank(name);
    Objects.requireNonNull(label, "label");
    Objects.requireNonNull(typeCode, "typeCode");
  }

  /** Creates a member with a numeric label. */
  public static WireTypeCodeUnionMember label(String name, long label, WireTypeCode typeCode) {
    return new WireTypeCodeUnionMember(name, OptionalLong.of(label), typeCode);
  }

  /** Creates the default member. */
  public static WireTypeCodeUnionMember defaultMember(String name, WireTypeCode typeCode) {
    return new WireTypeCodeUnionMember(name, OptionalLong.empty(), typeCode);
  }

  private static String requireNonBlank(String value) {
    Objects.requireNonNull(value, "name");
    if (value.isBlank()) {
      throw new IllegalArgumentException("union member name must not be blank");
    }
    return value;
  }
}
