package io.github.mundanej.mjo.typecode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Immutable CORBA wire TypeCode description for the G10 interoperability slice. */
public record WireTypeCode(
    WireTypeCodeKind kind,
    Optional<String> repositoryId,
    Optional<String> name,
    OptionalInt bound,
    Optional<WireTypeCode> contentType,
    List<WireTypeCodeMember> members,
    List<String> enumConstants,
    Optional<WireTypeCode> discriminatorType,
    List<WireTypeCodeUnionMember> unionMembers) {

  /** Creates a validated wire TypeCode. */
  public WireTypeCode {
    Objects.requireNonNull(kind, "kind");
    repositoryId = requireOptionalText(repositoryId, "repositoryId");
    name = requireOptionalText(name, "name");
    Objects.requireNonNull(bound, "bound");
    if (bound.isPresent() && bound.getAsInt() < 0) {
      throw new IllegalArgumentException("TypeCode bound must be nonnegative");
    }
    Objects.requireNonNull(contentType, "contentType");
    members = List.copyOf(Objects.requireNonNull(members, "members"));
    enumConstants = copyNames(enumConstants, "enum constant");
    Objects.requireNonNull(discriminatorType, "discriminatorType");
    unionMembers = List.copyOf(Objects.requireNonNull(unionMembers, "unionMembers"));
    validateShape(
        kind,
        repositoryId,
        name,
        bound,
        contentType,
        members,
        enumConstants,
        discriminatorType,
        unionMembers);
  }

  /** Creates a primitive or scalar TypeCode. */
  public static WireTypeCode primitive(WireTypeCodeKind kind) {
    return new WireTypeCode(
        kind,
        Optional.empty(),
        Optional.empty(),
        OptionalInt.empty(),
        Optional.empty(),
        List.of(),
        List.of(),
        Optional.empty(),
        List.of());
  }

  /** Creates a bounded or unbounded string TypeCode. */
  public static WireTypeCode string(int bound) {
    return boundedScalar(WireTypeCodeKind.STRING, bound);
  }

  /** Creates a bounded or unbounded wide-string TypeCode. */
  public static WireTypeCode wstring(int bound) {
    return boundedScalar(WireTypeCodeKind.WSTRING, bound);
  }

  /** Creates an object-reference TypeCode. */
  public static WireTypeCode objectReference(String repositoryId, String name) {
    return named(WireTypeCodeKind.OBJECT_REFERENCE, repositoryId, name, List.of(), List.of());
  }

  /** Creates a struct TypeCode. */
  public static WireTypeCode struct(
      String repositoryId, String name, List<WireTypeCodeMember> members) {
    return named(WireTypeCodeKind.STRUCT, repositoryId, name, members, List.of());
  }

  /** Creates an exception TypeCode. */
  public static WireTypeCode exception(
      String repositoryId, String name, List<WireTypeCodeMember> members) {
    return named(WireTypeCodeKind.EXCEPTION, repositoryId, name, members, List.of());
  }

  /** Creates an enum TypeCode. */
  public static WireTypeCode enumeration(
      String repositoryId, String name, List<String> enumConstants) {
    return named(WireTypeCodeKind.ENUM, repositoryId, name, List.of(), enumConstants);
  }

  /** Creates an alias TypeCode. */
  public static WireTypeCode alias(String repositoryId, String name, WireTypeCode contentType) {
    return namedContent(
        WireTypeCodeKind.ALIAS, repositoryId, name, contentType, OptionalInt.empty());
  }

  /** Creates a sequence TypeCode. A bound of zero means unbounded. */
  public static WireTypeCode sequence(WireTypeCode elementType, int bound) {
    return unnamedContent(WireTypeCodeKind.SEQUENCE, elementType, OptionalInt.of(bound));
  }

  /** Creates an array TypeCode. */
  public static WireTypeCode array(WireTypeCode elementType, int length) {
    return unnamedContent(WireTypeCodeKind.ARRAY, elementType, OptionalInt.of(length));
  }

  /** Creates a union TypeCode. */
  public static WireTypeCode union(
      String repositoryId,
      String name,
      WireTypeCode discriminatorType,
      List<WireTypeCodeUnionMember> members) {
    return new WireTypeCode(
        WireTypeCodeKind.UNION,
        Optional.of(repositoryId),
        Optional.of(name),
        OptionalInt.empty(),
        Optional.empty(),
        List.of(),
        List.of(),
        Optional.of(discriminatorType),
        members);
  }

  @Override
  public List<WireTypeCodeMember> members() {
    return List.copyOf(members);
  }

  @Override
  public List<String> enumConstants() {
    return List.copyOf(enumConstants);
  }

  @Override
  public List<WireTypeCodeUnionMember> unionMembers() {
    return List.copyOf(unionMembers);
  }

  private static WireTypeCode boundedScalar(WireTypeCodeKind kind, int bound) {
    return new WireTypeCode(
        kind,
        Optional.empty(),
        Optional.empty(),
        OptionalInt.of(bound),
        Optional.empty(),
        List.of(),
        List.of(),
        Optional.empty(),
        List.of());
  }

  private static WireTypeCode named(
      WireTypeCodeKind kind,
      String repositoryId,
      String name,
      List<WireTypeCodeMember> members,
      List<String> enumConstants) {
    return new WireTypeCode(
        kind,
        Optional.of(repositoryId),
        Optional.of(name),
        OptionalInt.empty(),
        Optional.empty(),
        members,
        enumConstants,
        Optional.empty(),
        List.of());
  }

  private static WireTypeCode namedContent(
      WireTypeCodeKind kind,
      String repositoryId,
      String name,
      WireTypeCode contentType,
      OptionalInt bound) {
    return new WireTypeCode(
        kind,
        Optional.of(repositoryId),
        Optional.of(name),
        bound,
        Optional.of(contentType),
        List.of(),
        List.of(),
        Optional.empty(),
        List.of());
  }

  private static WireTypeCode unnamedContent(
      WireTypeCodeKind kind, WireTypeCode contentType, OptionalInt bound) {
    return new WireTypeCode(
        kind,
        Optional.empty(),
        Optional.empty(),
        bound,
        Optional.of(contentType),
        List.of(),
        List.of(),
        Optional.empty(),
        List.of());
  }

  private static Optional<String> requireOptionalText(Optional<String> value, String name) {
    Objects.requireNonNull(value, name);
    value.ifPresent(text -> requireNonBlank(text, name));
    return value;
  }

  private static List<String> copyNames(List<String> names, String label) {
    Objects.requireNonNull(names, label);
    for (String current : names) {
      requireNonBlank(current, label);
    }
    return List.copyOf(names);
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }

  private static void validateShape(
      WireTypeCodeKind kind,
      Optional<String> repositoryId,
      Optional<String> name,
      OptionalInt bound,
      Optional<WireTypeCode> contentType,
      List<WireTypeCodeMember> members,
      List<String> enumConstants,
      Optional<WireTypeCode> discriminatorType,
      List<WireTypeCodeUnionMember> unionMembers) {
    switch (kind) {
      case STRING, WSTRING -> requireOnlyBound(bound, repositoryId, name, contentType);
      case OBJECT_REFERENCE -> requireNamedOnly(repositoryId, name, members, enumConstants);
      case STRUCT, EXCEPTION -> requireNamedMembers(repositoryId, name, members, enumConstants);
      case ENUM -> requireEnum(repositoryId, name, enumConstants, members);
      case ALIAS -> requireNamedContent(repositoryId, name, contentType);
      case SEQUENCE, ARRAY -> requireContentOnly(kind, bound, contentType);
      case UNION -> requireUnion(repositoryId, name, discriminatorType, unionMembers);
      default -> requireScalar(repositoryId, name, bound, contentType, members, enumConstants);
    }
    if (kind != WireTypeCodeKind.UNION && discriminatorType.isPresent()) {
      throw new IllegalArgumentException(kind + " TypeCode must not have a discriminator");
    }
    if (kind != WireTypeCodeKind.UNION && !unionMembers.isEmpty()) {
      throw new IllegalArgumentException(kind + " TypeCode must not have union members");
    }
  }

  private static void requireOnlyBound(
      OptionalInt bound,
      Optional<String> repositoryId,
      Optional<String> name,
      Optional<WireTypeCode> contentType) {
    if (bound.isEmpty()
        || repositoryId.isPresent()
        || name.isPresent()
        || contentType.isPresent()) {
      throw new IllegalArgumentException("string TypeCode shape is invalid");
    }
  }

  private static void requireNamedOnly(
      Optional<String> repositoryId,
      Optional<String> name,
      List<WireTypeCodeMember> members,
      List<String> enumConstants) {
    if (repositoryId.isEmpty()
        || name.isEmpty()
        || !members.isEmpty()
        || !enumConstants.isEmpty()) {
      throw new IllegalArgumentException("named object TypeCode shape is invalid");
    }
  }

  private static void requireNamedMembers(
      Optional<String> repositoryId,
      Optional<String> name,
      List<WireTypeCodeMember> members,
      List<String> enumConstants) {
    if (repositoryId.isEmpty() || name.isEmpty() || members.isEmpty() || !enumConstants.isEmpty()) {
      throw new IllegalArgumentException("aggregate TypeCode shape is invalid");
    }
  }

  private static void requireEnum(
      Optional<String> repositoryId,
      Optional<String> name,
      List<String> enumConstants,
      List<WireTypeCodeMember> members) {
    if (repositoryId.isEmpty() || name.isEmpty() || enumConstants.isEmpty() || !members.isEmpty()) {
      throw new IllegalArgumentException("enum TypeCode shape is invalid");
    }
  }

  private static void requireNamedContent(
      Optional<String> repositoryId, Optional<String> name, Optional<WireTypeCode> contentType) {
    if (repositoryId.isEmpty() || name.isEmpty() || contentType.isEmpty()) {
      throw new IllegalArgumentException("alias TypeCode shape is invalid");
    }
  }

  private static void requireContentOnly(
      WireTypeCodeKind kind, OptionalInt bound, Optional<WireTypeCode> contentType) {
    if (bound.isEmpty() || contentType.isEmpty()) {
      throw new IllegalArgumentException(kind + " TypeCode shape is invalid");
    }
    if (kind == WireTypeCodeKind.ARRAY && bound.getAsInt() == 0) {
      throw new IllegalArgumentException("array TypeCode length must be positive");
    }
  }

  private static void requireUnion(
      Optional<String> repositoryId,
      Optional<String> name,
      Optional<WireTypeCode> discriminatorType,
      List<WireTypeCodeUnionMember> unionMembers) {
    if (repositoryId.isEmpty()
        || name.isEmpty()
        || discriminatorType.isEmpty()
        || unionMembers.isEmpty()) {
      throw new IllegalArgumentException("union TypeCode shape is invalid");
    }
  }

  private static void requireScalar(
      Optional<String> repositoryId,
      Optional<String> name,
      OptionalInt bound,
      Optional<WireTypeCode> contentType,
      List<WireTypeCodeMember> members,
      List<String> enumConstants) {
    if (repositoryId.isPresent()
        || name.isPresent()
        || bound.isPresent()
        || contentType.isPresent()
        || !members.isEmpty()
        || !enumConstants.isEmpty()) {
      throw new IllegalArgumentException("scalar TypeCode shape is invalid");
    }
  }
}
