package io.github.mundanej.mjo.typecode;

import java.util.Objects;

/**
 * Static descriptor for an IDL operation parameter.
 *
 * @param name IDL parameter name
 * @param mode parameter passing mode
 * @param type parameter type
 */
public record IdlParameterDescriptor(String name, IdlParameterMode mode, IdlTypeReference type) {

  /** Creates a validated parameter descriptor. */
  public IdlParameterDescriptor {
    name = requireNonBlank(name, "name");
    Objects.requireNonNull(mode, "mode");
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
