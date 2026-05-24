package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * IDL interface model derived from an eligible Java remote interface.
 *
 * @param name IDL interface identifier
 * @param scopedName deterministic IDL scoped name
 * @param javaBinaryName Java binary name for the source remote interface when known
 * @param operations operations in Java declaration order
 * @param baseScopedNames inherited IDL interface scoped names in deterministic order
 */
public record RmiIdlInterface(
    String name,
    String scopedName,
    Optional<String> javaBinaryName,
    List<RmiIdlOperation> operations,
    List<String> baseScopedNames) {

  /** Creates an immutable interface model. */
  public RmiIdlInterface {
    name = requireNonBlank(name, "name");
    scopedName = requireNonBlank(scopedName, "scopedName");
    Objects.requireNonNull(javaBinaryName, "javaBinaryName")
        .ifPresent(value -> requireNonBlank(value, "javaBinaryName"));
    operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
    baseScopedNames =
        List.copyOf(Objects.requireNonNull(baseScopedNames, "baseScopedNames")).stream()
            .map(value -> requireNonBlank(value, "baseScopedName"))
            .toList();
  }

  /** Creates an interface model without inherited interfaces. */
  public RmiIdlInterface(
      String name,
      String scopedName,
      Optional<String> javaBinaryName,
      List<RmiIdlOperation> operations) {
    this(name, scopedName, javaBinaryName, operations, List.of());
  }

  /** Creates an interface model without Java binary-name metadata. */
  public RmiIdlInterface(String name, String scopedName, List<RmiIdlOperation> operations) {
    this(name, scopedName, Optional.empty(), operations, List.of());
  }

  @Override
  public List<String> baseScopedNames() {
    return List.copyOf(baseScopedNames);
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
