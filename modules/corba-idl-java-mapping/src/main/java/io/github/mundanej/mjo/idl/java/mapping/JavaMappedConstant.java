package io.github.mundanej.mjo.idl.java.mapping;

import java.util.Objects;

/**
 * Java constant selected from an evaluated IDL constant.
 *
 * @param javaType Java type spelling
 * @param name Java constant name
 * @param initializer Java source initializer expression
 */
public record JavaMappedConstant(String javaType, String name, String initializer) {

  /** Creates a validated mapped constant. */
  public JavaMappedConstant {
    javaType = requireNonBlank(javaType, "javaType");
    name = requireNonBlank(name, "name");
    initializer = requireNonBlank(initializer, "initializer");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
