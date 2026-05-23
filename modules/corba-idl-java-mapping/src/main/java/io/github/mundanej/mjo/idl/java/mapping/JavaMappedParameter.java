package io.github.mundanej.mjo.idl.java.mapping;

import io.github.mundanej.mjo.idl.ast.IdlParameterDirection;
import java.util.Objects;

/**
 * Java method parameter selected from an IDL operation parameter.
 *
 * @param javaType Java type spelling
 * @param name Java parameter name
 * @param direction IDL parameter direction
 */
public record JavaMappedParameter(String javaType, String name, IdlParameterDirection direction) {

  /** Creates a validated mapped parameter. */
  public JavaMappedParameter {
    javaType = requireNonBlank(javaType, "javaType");
    name = requireNonBlank(name, "name");
    Objects.requireNonNull(direction, "direction");
  }

  /** Creates an `in` parameter for compatibility with the earlier mapping model. */
  public JavaMappedParameter(String javaType, String name) {
    this(javaType, name, IdlParameterDirection.IN);
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
