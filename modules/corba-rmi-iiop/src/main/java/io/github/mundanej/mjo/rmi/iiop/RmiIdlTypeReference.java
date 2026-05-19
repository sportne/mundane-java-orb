package io.github.mundanej.mjo.rmi.iiop;

import java.util.Objects;
import java.util.Optional;

/**
 * IDL type reference produced by Java-to-IDL mapping.
 *
 * @param kind type-reference category
 * @param name IDL built-in type name or scoped name
 * @param javaBinaryName Java binary name for declared value references
 * @param elementType sequence element type when {@code kind} is {@link RmiIdlTypeKind#SEQUENCE}
 */
public record RmiIdlTypeReference(
    RmiIdlTypeKind kind,
    String name,
    Optional<String> javaBinaryName,
    Optional<RmiIdlTypeReference> elementType) {

  /** Creates an immutable IDL type reference. */
  public RmiIdlTypeReference {
    Objects.requireNonNull(kind, "kind");
    name = requireNonBlank(name, "name");
    Objects.requireNonNull(javaBinaryName, "javaBinaryName");
    Objects.requireNonNull(elementType, "elementType");
    if (kind != RmiIdlTypeKind.DECLARED_VALUE && javaBinaryName.isPresent()) {
      throw new IllegalArgumentException("javaBinaryName is only valid for declared values");
    }
    if (kind != RmiIdlTypeKind.SEQUENCE && elementType.isPresent()) {
      throw new IllegalArgumentException("elementType is only valid for sequences");
    }
  }

  /** Returns the IDL void pseudo-type. */
  public static RmiIdlTypeReference voidType() {
    return new RmiIdlTypeReference(RmiIdlTypeKind.VOID, "void", Optional.empty(), Optional.empty());
  }

  /** Returns an IDL built-in type reference. */
  public static RmiIdlTypeReference builtin(String name) {
    return new RmiIdlTypeReference(
        RmiIdlTypeKind.BUILTIN, name, Optional.empty(), Optional.empty());
  }

  /** Returns a declared value or remote interface scoped reference. */
  public static RmiIdlTypeReference declaredValue(String scopedName, String javaBinaryName) {
    return new RmiIdlTypeReference(
        RmiIdlTypeKind.DECLARED_VALUE,
        scopedName,
        Optional.of(requireNonBlank(javaBinaryName, "javaBinaryName")),
        Optional.empty());
  }

  /** Returns a sequence type reference. */
  public static RmiIdlTypeReference sequenceOf(RmiIdlTypeReference elementType) {
    Objects.requireNonNull(elementType, "elementType");
    return new RmiIdlTypeReference(
        RmiIdlTypeKind.SEQUENCE,
        "sequence<" + elementType.name() + ">",
        Optional.empty(),
        Optional.of(elementType));
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
