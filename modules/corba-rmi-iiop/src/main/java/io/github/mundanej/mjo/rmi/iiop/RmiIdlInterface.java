package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;

/**
 * IDL interface model derived from an eligible Java remote interface.
 *
 * @param name IDL interface identifier
 * @param scopedName deterministic IDL scoped name
 * @param operations operations in Java declaration order
 */
public record RmiIdlInterface(String name, String scopedName, List<RmiIdlOperation> operations) {

  /** Creates an immutable interface model. */
  public RmiIdlInterface {
    name = requireNonBlank(name, "name");
    scopedName = requireNonBlank(scopedName, "scopedName");
    operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
