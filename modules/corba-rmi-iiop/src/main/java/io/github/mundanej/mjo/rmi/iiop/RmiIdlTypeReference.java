package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * IDL type reference produced by Java-to-IDL mapping.
 *
 * @param kind type-reference category
 * @param name IDL built-in type name or scoped name
 * @param javaBinaryName Java binary name for declared value references
 * @param elementType sequence element type when {@code kind} is {@link RmiIdlTypeKind#SEQUENCE}
 * @param valueMembers explicit declared-value member metadata
 */
public record RmiIdlTypeReference(
    RmiIdlTypeKind kind,
    String name,
    Optional<String> javaBinaryName,
    Optional<RmiIdlTypeReference> elementType,
    List<RmiIdlValueMember> valueMembers) {

  /** Creates an immutable IDL type reference. */
  public RmiIdlTypeReference {
    Objects.requireNonNull(kind, "kind");
    name = requireNonBlank(name, "name");
    Objects.requireNonNull(javaBinaryName, "javaBinaryName");
    Objects.requireNonNull(elementType, "elementType");
    valueMembers = List.copyOf(Objects.requireNonNull(valueMembers, "valueMembers"));
    if (kind != RmiIdlTypeKind.DECLARED_VALUE
        && kind != RmiIdlTypeKind.REMOTE_OBJECT
        && javaBinaryName.isPresent()) {
      throw new IllegalArgumentException(
          "javaBinaryName is only valid for declared values and remote objects");
    }
    if ((kind == RmiIdlTypeKind.DECLARED_VALUE || kind == RmiIdlTypeKind.REMOTE_OBJECT)
        && javaBinaryName.isEmpty()) {
      throw new IllegalArgumentException(
          "javaBinaryName is required for declared values and remote objects");
    }
    if (kind != RmiIdlTypeKind.SEQUENCE && elementType.isPresent()) {
      throw new IllegalArgumentException("elementType is only valid for sequences");
    }
    if (kind != RmiIdlTypeKind.DECLARED_VALUE && !valueMembers.isEmpty()) {
      throw new IllegalArgumentException("valueMembers are only valid for declared values");
    }
  }

  /** Creates an immutable IDL type reference without declared-value members. */
  public RmiIdlTypeReference(
      RmiIdlTypeKind kind,
      String name,
      Optional<String> javaBinaryName,
      Optional<RmiIdlTypeReference> elementType) {
    this(kind, name, javaBinaryName, elementType, List.of());
  }

  /** Returns the IDL void pseudo-type. */
  public static RmiIdlTypeReference voidType() {
    return new RmiIdlTypeReference(
        RmiIdlTypeKind.VOID, "void", Optional.empty(), Optional.empty(), List.of());
  }

  /** Returns an IDL built-in type reference. */
  public static RmiIdlTypeReference builtin(String name) {
    return new RmiIdlTypeReference(
        RmiIdlTypeKind.BUILTIN, name, Optional.empty(), Optional.empty(), List.of());
  }

  /** Returns a declared value or remote interface scoped reference. */
  public static RmiIdlTypeReference declaredValue(String scopedName, String javaBinaryName) {
    return declaredValue(scopedName, javaBinaryName, List.of());
  }

  /** Returns a declared value scoped reference with explicit member metadata. */
  public static RmiIdlTypeReference declaredValue(
      String scopedName, String javaBinaryName, List<RmiIdlValueMember> valueMembers) {
    return new RmiIdlTypeReference(
        RmiIdlTypeKind.DECLARED_VALUE,
        scopedName,
        Optional.of(requireNonBlank(javaBinaryName, "javaBinaryName")),
        Optional.empty(),
        valueMembers);
  }

  /** Returns a remote object scoped reference. */
  public static RmiIdlTypeReference remoteObject(String scopedName, String javaBinaryName) {
    return new RmiIdlTypeReference(
        RmiIdlTypeKind.REMOTE_OBJECT,
        scopedName,
        Optional.of(requireNonBlank(javaBinaryName, "javaBinaryName")),
        Optional.empty(),
        List.of());
  }

  /** Returns a sequence type reference. */
  public static RmiIdlTypeReference sequenceOf(RmiIdlTypeReference elementType) {
    Objects.requireNonNull(elementType, "elementType");
    return new RmiIdlTypeReference(
        RmiIdlTypeKind.SEQUENCE,
        "sequence<" + elementType.name() + ">",
        Optional.empty(),
        Optional.of(elementType),
        List.of());
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
