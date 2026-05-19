package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;

/**
 * IDL module model derived from a Java package segment.
 *
 * @param name IDL module identifier
 * @param scopedName deterministic IDL scoped name
 * @param modules nested modules in declaration order
 * @param interfaces interfaces declared directly in this module
 */
public record RmiIdlModule(
    String name, String scopedName, List<RmiIdlModule> modules, List<RmiIdlInterface> interfaces) {

  /** Creates an immutable module model. */
  public RmiIdlModule {
    name = requireNonBlank(name, "name");
    scopedName = requireNonBlank(scopedName, "scopedName");
    modules = List.copyOf(Objects.requireNonNull(modules, "modules"));
    interfaces = List.copyOf(Objects.requireNonNull(interfaces, "interfaces"));
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
