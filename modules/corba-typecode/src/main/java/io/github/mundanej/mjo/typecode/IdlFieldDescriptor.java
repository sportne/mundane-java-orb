package io.github.mundanej.mjo.typecode;

import java.util.Objects;

/**
 * Static descriptor for an IDL struct or exception field.
 *
 * @param name IDL field name
 * @param type field type
 */
public record IdlFieldDescriptor(String name, IdlTypeReference type) {

  /** Creates a validated field descriptor. */
  public IdlFieldDescriptor {
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
