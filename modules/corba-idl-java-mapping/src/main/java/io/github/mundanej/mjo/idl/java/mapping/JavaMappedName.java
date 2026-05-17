package io.github.mundanej.mjo.idl.java.mapping;

import java.util.Objects;

/**
 * Java package and simple type name selected for an IDL declaration.
 *
 * @param packageName Java package name, or empty for the default package
 * @param simpleName Java simple type name
 */
public record JavaMappedName(String packageName, String simpleName) {

  /** Creates a validated mapped Java name. */
  public JavaMappedName {
    Objects.requireNonNull(packageName, "packageName");
    simpleName = requireNonBlank(simpleName, "simpleName");
  }

  /** Returns the Java qualified name. */
  public String qualifiedName() {
    return packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
