package io.github.mundanej.mjo.rmi.iiop;

import java.util.Objects;
import java.util.Optional;

/**
 * Explicit Java type reference for remote-interface eligibility checks.
 *
 * @param kind recognized type-reference shape
 * @param name stable display name or Java binary name for declared types
 * @param componentType array component type when {@code kind} is {@link RmiJavaTypeKind#ARRAY}
 */
public record RmiJavaTypeReference(
    RmiJavaTypeKind kind, String name, Optional<RmiJavaTypeReference> componentType) {

  /** Creates an immutable Java type reference. */
  public RmiJavaTypeReference {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(componentType, "componentType");
    if (kind != RmiJavaTypeKind.ARRAY && componentType.isPresent()) {
      throw new IllegalArgumentException("componentType is only valid for array type references");
    }
  }

  /** Creates a Java type reference without structured component metadata. */
  public RmiJavaTypeReference(RmiJavaTypeKind kind, String name) {
    this(kind, name, Optional.empty());
  }

  /** Returns the Java void pseudo-type. */
  public static RmiJavaTypeReference voidType() {
    return new RmiJavaTypeReference(RmiJavaTypeKind.VOID, "void");
  }

  /** Returns a Java primitive type by keyword name. */
  public static RmiJavaTypeReference primitive(String name) {
    return new RmiJavaTypeReference(RmiJavaTypeKind.PRIMITIVE, name);
  }

  /** Returns a Java declared type by binary name. */
  public static RmiJavaTypeReference declared(String binaryName) {
    return new RmiJavaTypeReference(RmiJavaTypeKind.DECLARED, binaryName);
  }

  /** Returns a Java remote object-reference type by binary name. */
  public static RmiJavaTypeReference remote(String binaryName) {
    return new RmiJavaTypeReference(RmiJavaTypeKind.REMOTE, binaryName);
  }

  /** Returns an unsupported Java array type shape. */
  public static RmiJavaTypeReference array(String displayName) {
    return new RmiJavaTypeReference(RmiJavaTypeKind.ARRAY, displayName);
  }

  /** Returns a Java array type shape with explicit component metadata. */
  public static RmiJavaTypeReference arrayOf(RmiJavaTypeReference componentType) {
    Objects.requireNonNull(componentType, "componentType");
    return new RmiJavaTypeReference(
        RmiJavaTypeKind.ARRAY, componentType.displayName() + "[]", Optional.of(componentType));
  }

  /** Returns an unsupported Java generic type shape. */
  public static RmiJavaTypeReference generic(String displayName) {
    return new RmiJavaTypeReference(RmiJavaTypeKind.GENERIC, displayName);
  }

  /** Returns an unsupported Java wildcard type shape. */
  public static RmiJavaTypeReference wildcard(String displayName) {
    return new RmiJavaTypeReference(RmiJavaTypeKind.WILDCARD, displayName);
  }

  /** Returns a stable human-readable type name for diagnostics. */
  public String displayName() {
    return name.isBlank() ? "<blank>" : name;
  }
}
