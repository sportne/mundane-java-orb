package io.github.mundanej.mjo.idl.java.mapping;

import java.util.Objects;

/**
 * Java field or exception member selected from an IDL field.
 *
 * @param javaType Java type spelling
 * @param name Java member name
 */
public record JavaMappedField(String javaType, String name) {

  /** Creates a validated mapped field. */
  public JavaMappedField {
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
