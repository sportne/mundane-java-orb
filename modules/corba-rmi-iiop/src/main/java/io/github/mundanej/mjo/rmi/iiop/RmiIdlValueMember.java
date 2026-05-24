package io.github.mundanej.mjo.rmi.iiop;

import java.util.Objects;

/**
 * Explicit value-member metadata for bounded RMI-IIOP declared value payloads.
 *
 * @param name deterministic member name
 * @param type member IDL type reference
 */
public record RmiIdlValueMember(String name, RmiIdlTypeReference type) {

  /** Creates an immutable value-member descriptor. */
  public RmiIdlValueMember {
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
