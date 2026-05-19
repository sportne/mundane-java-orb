package io.github.mundanej.mjo.rmi.iiop;

import java.util.Objects;

/**
 * IDL operation parameter model derived from a Java parameter declaration.
 *
 * @param name IDL parameter identifier
 * @param type mapped IDL parameter type
 */
public record RmiIdlParameter(String name, RmiIdlTypeReference type) {

  /** Creates an immutable parameter model. */
  public RmiIdlParameter {
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
