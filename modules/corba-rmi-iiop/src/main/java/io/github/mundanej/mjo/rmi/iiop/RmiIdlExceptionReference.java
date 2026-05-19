package io.github.mundanej.mjo.rmi.iiop;

import java.util.Objects;

/**
 * IDL exception reference derived from a Java checked exception declaration.
 *
 * @param javaBinaryName Java exception binary name
 * @param scopedName deterministic IDL scoped name
 */
public record RmiIdlExceptionReference(String javaBinaryName, String scopedName) {

  /** Creates an immutable exception reference. */
  public RmiIdlExceptionReference {
    javaBinaryName = requireNonBlank(javaBinaryName, "javaBinaryName");
    scopedName = requireNonBlank(scopedName, "scopedName");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
