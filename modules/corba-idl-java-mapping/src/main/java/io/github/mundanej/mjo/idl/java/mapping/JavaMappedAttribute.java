package io.github.mundanej.mjo.idl.java.mapping;

import java.util.Objects;

/**
 * Java accessor pair selected from an IDL attribute.
 *
 * @param javaType Java type spelling
 * @param name Java property name
 * @param readonly whether the IDL attribute is readonly
 */
public record JavaMappedAttribute(String javaType, String name, boolean readonly) {

  /** Creates a validated mapped attribute. */
  public JavaMappedAttribute {
    javaType = requireNonBlank(javaType, "javaType");
    name = requireNonBlank(name, "name");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
