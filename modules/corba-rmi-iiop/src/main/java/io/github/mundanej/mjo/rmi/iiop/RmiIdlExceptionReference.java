package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;

/**
 * IDL exception reference derived from a Java checked exception declaration.
 *
 * @param javaBinaryName Java exception binary name
 * @param scopedName deterministic IDL scoped name
 * @param fields explicit user-exception payload fields in IDL order
 */
public record RmiIdlExceptionReference(
    String javaBinaryName, String scopedName, List<RmiIdlValueMember> fields) {

  /** Creates an immutable exception reference. */
  public RmiIdlExceptionReference {
    javaBinaryName = requireNonBlank(javaBinaryName, "javaBinaryName");
    scopedName = requireNonBlank(scopedName, "scopedName");
    fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
  }

  /** Creates an exception reference without payload fields. */
  public RmiIdlExceptionReference(String javaBinaryName, String scopedName) {
    this(javaBinaryName, scopedName, List.of());
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
