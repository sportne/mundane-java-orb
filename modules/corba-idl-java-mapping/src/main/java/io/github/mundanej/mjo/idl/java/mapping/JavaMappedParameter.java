package io.github.mundanej.mjo.idl.java.mapping;

import java.util.Objects;

/**
 * Java method parameter selected from an IDL operation parameter.
 *
 * @param javaType Java type spelling
 * @param name Java parameter name
 */
public record JavaMappedParameter(String javaType, String name) {

  /** Creates a validated mapped parameter. */
  public JavaMappedParameter {
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
