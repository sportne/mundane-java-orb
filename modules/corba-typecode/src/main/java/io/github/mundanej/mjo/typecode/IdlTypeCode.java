package io.github.mundanej.mjo.typecode;

import io.github.mundanej.mjo.repositoryid.RepositoryId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Immutable local TypeCode metadata backed by generated IDL descriptors.
 *
 * <p>This model describes the static type of local Any payloads. It does not encode the full CORBA
 * wire representation of a TypeCode.
 *
 * @param kind TypeCode kind
 * @param idlName IDL spelling or absolute scoped name
 * @param javaName mapped Java spelling
 * @param repositoryId repository ID for generated user-defined IDL declarations
 * @param members struct or exception members in IDL order
 * @param enumConstants enum constants in IDL order
 * @param elementType sequence element TypeCode
 */
public record IdlTypeCode(
    IdlTypeCodeKind kind,
    String idlName,
    String javaName,
    Optional<RepositoryId> repositoryId,
    List<IdlTypeCodeMember> members,
    List<String> enumConstants,
    Optional<IdlTypeCode> elementType) {

  public static final IdlTypeCode VOID = primitive(IdlTypeCodeKind.VOID, "void", "void");
  public static final IdlTypeCode BOOLEAN =
      primitive(IdlTypeCodeKind.BOOLEAN, "boolean", "boolean");
  public static final IdlTypeCode OCTET = primitive(IdlTypeCodeKind.OCTET, "octet", "int");
  public static final IdlTypeCode CHAR = primitive(IdlTypeCodeKind.CHAR, "char", "char");
  public static final IdlTypeCode SHORT = primitive(IdlTypeCodeKind.SHORT, "short", "short");
  public static final IdlTypeCode UNSIGNED_SHORT =
      primitive(IdlTypeCodeKind.UNSIGNED_SHORT, "unsigned short", "int");
  public static final IdlTypeCode LONG = primitive(IdlTypeCodeKind.LONG, "long", "int");
  public static final IdlTypeCode UNSIGNED_LONG =
      primitive(IdlTypeCodeKind.UNSIGNED_LONG, "unsigned long", "long");
  public static final IdlTypeCode LONG_LONG =
      primitive(IdlTypeCodeKind.LONG_LONG, "long long", "long");
  public static final IdlTypeCode UNSIGNED_LONG_LONG =
      primitive(IdlTypeCodeKind.UNSIGNED_LONG_LONG, "unsigned long long", "java.math.BigInteger");
  public static final IdlTypeCode FLOAT = primitive(IdlTypeCodeKind.FLOAT, "float", "float");
  public static final IdlTypeCode DOUBLE = primitive(IdlTypeCodeKind.DOUBLE, "double", "double");
  public static final IdlTypeCode LONG_DOUBLE =
      primitive(IdlTypeCodeKind.LONG_DOUBLE, "long double", "byte[]");
  public static final IdlTypeCode STRING =
      primitive(IdlTypeCodeKind.STRING, "string", "java.lang.String");

  /** Creates a validated TypeCode. */
  public IdlTypeCode {
    Objects.requireNonNull(kind, "kind");
    idlName = requireNonBlank(idlName, "idlName");
    javaName = requireNonBlank(javaName, "javaName");
    Objects.requireNonNull(repositoryId, "repositoryId");
    members = List.copyOf(Objects.requireNonNull(members, "members"));
    enumConstants = List.copyOf(Objects.requireNonNull(enumConstants, "enumConstants"));
    for (String constant : enumConstants) {
      requireNonBlank(constant, "enumConstant");
    }
    Objects.requireNonNull(elementType, "elementType");
    validateShape(kind, repositoryId, members, enumConstants, elementType);
  }

  /** Creates a primitive TypeCode for the supplied scalar kind. */
  public static IdlTypeCode primitive(IdlTypeCodeKind kind, String idlName, String javaName) {
    Objects.requireNonNull(kind, "kind");
    if (isAggregateKind(kind) || kind == IdlTypeCodeKind.SEQUENCE || kind == IdlTypeCodeKind.ENUM) {
      throw new IllegalArgumentException("primitive TypeCode kind is not scalar: " + kind);
    }
    return new IdlTypeCode(
        kind, idlName, javaName, Optional.empty(), List.of(), List.of(), Optional.empty());
  }

  /** Creates a TypeCode from a generated descriptor. */
  public static IdlTypeCode fromDescriptor(IdlGeneratedTypeDescriptor descriptor) {
    return fromDescriptor(descriptor, IdlTypeCode::fromTypeReference);
  }

  /** Creates a TypeCode from a generated descriptor using a resolver for generated field types. */
  public static IdlTypeCode fromDescriptor(
      IdlGeneratedTypeDescriptor descriptor, Function<IdlTypeReference, IdlTypeCode> typeResolver) {
    Objects.requireNonNull(descriptor, "descriptor");
    Objects.requireNonNull(typeResolver, "typeResolver");
    IdlTypeCodeKind typeCodeKind = generatedKind(descriptor.kind());
    return switch (typeCodeKind) {
      case STRUCT, EXCEPTION ->
          new IdlTypeCode(
              typeCodeKind,
              descriptor.idlScopedName(),
              descriptor.javaName(),
              Optional.of(descriptor.repositoryId()),
              membersFromFields(descriptor.fields(), typeResolver),
              List.of(),
              Optional.empty());
      case ENUM ->
          new IdlTypeCode(
              IdlTypeCodeKind.ENUM,
              descriptor.idlScopedName(),
              descriptor.javaName(),
              Optional.of(descriptor.repositoryId()),
              List.of(),
              descriptor.enumConstants(),
              Optional.empty());
      case INTERFACE ->
          new IdlTypeCode(
              IdlTypeCodeKind.INTERFACE,
              descriptor.idlScopedName(),
              descriptor.javaName(),
              Optional.of(descriptor.repositoryId()),
              List.of(),
              List.of(),
              Optional.empty());
      default ->
          throw new IllegalArgumentException("unsupported descriptor kind: " + descriptor.kind());
    };
  }

  /** Creates a TypeCode from a generated type reference. */
  public static IdlTypeCode fromTypeReference(IdlTypeReference reference) {
    Objects.requireNonNull(reference, "reference");
    if (reference.kind() == IdlTypeKind.PRIMITIVE) {
      return primitiveFromName(reference.idlName());
    }
    if (reference.kind() != IdlTypeKind.INTERFACE) {
      throw new IllegalArgumentException(
          "generated aggregate TypeCode reference requires a descriptor resolver: "
              + reference.idlName());
    }
    return new IdlTypeCode(
        IdlTypeCodeKind.INTERFACE,
        reference.idlName(),
        reference.javaName(),
        requireRepositoryId(reference),
        List.of(),
        List.of(),
        Optional.empty());
  }

  /** Creates a sequence TypeCode with an unbounded local element type. */
  public static IdlTypeCode sequenceOf(IdlTypeCode elementType, String idlName, String javaName) {
    if (elementType == null) {
      throw new IllegalArgumentException("elementType must not be null");
    }
    return new IdlTypeCode(
        IdlTypeCodeKind.SEQUENCE,
        idlName,
        javaName,
        Optional.empty(),
        List.of(),
        List.of(),
        Optional.of(elementType));
  }

  /** Returns true when this TypeCode describes a struct or exception aggregate. */
  public boolean isAggregate() {
    return kind == IdlTypeCodeKind.STRUCT || kind == IdlTypeCodeKind.EXCEPTION;
  }

  private static Optional<RepositoryId> requireRepositoryId(IdlTypeReference reference) {
    if (reference.repositoryId().isEmpty()) {
      throw new IllegalArgumentException(
          "generated TypeCode reference requires a repository ID: " + reference.idlName());
    }
    return reference.repositoryId();
  }

  private static List<IdlTypeCodeMember> membersFromFields(
      List<IdlFieldDescriptor> fields, Function<IdlTypeReference, IdlTypeCode> typeResolver) {
    List<IdlTypeCodeMember> result = new ArrayList<>(fields.size());
    for (IdlFieldDescriptor field : fields) {
      result.add(new IdlTypeCodeMember(field.name(), typeResolver.apply(field.type())));
    }
    return result;
  }

  private static IdlTypeCode primitiveFromName(String idlName) {
    return switch (idlName) {
      case "void" -> VOID;
      case "boolean" -> BOOLEAN;
      case "octet" -> OCTET;
      case "char" -> CHAR;
      case "short" -> SHORT;
      case "unsigned short" -> UNSIGNED_SHORT;
      case "long" -> LONG;
      case "unsigned long" -> UNSIGNED_LONG;
      case "long long" -> LONG_LONG;
      case "unsigned long long" -> UNSIGNED_LONG_LONG;
      case "float" -> FLOAT;
      case "double" -> DOUBLE;
      case "long double" -> LONG_DOUBLE;
      case "string" -> STRING;
      default -> throw new IllegalArgumentException("unsupported primitive TypeCode: " + idlName);
    };
  }

  private static IdlTypeCodeKind generatedKind(IdlTypeKind kind) {
    return switch (kind) {
      case INTERFACE -> IdlTypeCodeKind.INTERFACE;
      case STRUCT -> IdlTypeCodeKind.STRUCT;
      case ENUM -> IdlTypeCodeKind.ENUM;
      case EXCEPTION -> IdlTypeCodeKind.EXCEPTION;
      case PRIMITIVE, VOID ->
          throw new IllegalArgumentException("not a generated TypeCode: " + kind);
    };
  }

  private static void validateShape(
      IdlTypeCodeKind kind,
      Optional<RepositoryId> repositoryId,
      List<IdlTypeCodeMember> members,
      List<String> enumConstants,
      Optional<IdlTypeCode> elementType) {
    if (isAggregateKind(kind)) {
      requireRepositoryId(repositoryId, kind);
      if (members.isEmpty()) {
        throw new IllegalArgumentException(kind + " TypeCode requires at least one member");
      }
      requireEmpty(enumConstants, kind + " TypeCode must not have enum constants");
      requireEmpty(elementType, kind + " TypeCode must not have an element type");
      return;
    }
    if (kind == IdlTypeCodeKind.ENUM) {
      requireRepositoryId(repositoryId, kind);
      if (enumConstants.isEmpty()) {
        throw new IllegalArgumentException("ENUM TypeCode requires at least one constant");
      }
      requireEmpty(members, "ENUM TypeCode must not have members");
      requireEmpty(elementType, "ENUM TypeCode must not have an element type");
      return;
    }
    if (kind == IdlTypeCodeKind.SEQUENCE) {
      requireEmpty(repositoryId, "SEQUENCE TypeCode must not have a repository ID");
      requireEmpty(members, "SEQUENCE TypeCode must not have members");
      requireEmpty(enumConstants, "SEQUENCE TypeCode must not have enum constants");
      if (elementType.isEmpty()) {
        throw new IllegalArgumentException("SEQUENCE TypeCode requires an element type");
      }
      return;
    }
    if (kind == IdlTypeCodeKind.INTERFACE) {
      requireRepositoryId(repositoryId, kind);
      requireEmpty(members, "INTERFACE TypeCode must not have members");
      requireEmpty(enumConstants, "INTERFACE TypeCode must not have enum constants");
      requireEmpty(elementType, "INTERFACE TypeCode must not have an element type");
      return;
    }
    requireEmpty(repositoryId, kind + " TypeCode must not have a repository ID");
    requireEmpty(members, kind + " TypeCode must not have members");
    requireEmpty(enumConstants, kind + " TypeCode must not have enum constants");
    requireEmpty(elementType, kind + " TypeCode must not have an element type");
  }

  private static boolean isAggregateKind(IdlTypeCodeKind kind) {
    return kind == IdlTypeCodeKind.STRUCT || kind == IdlTypeCodeKind.EXCEPTION;
  }

  private static void requireRepositoryId(
      Optional<RepositoryId> repositoryId, IdlTypeCodeKind kind) {
    if (repositoryId.isEmpty()) {
      throw new IllegalArgumentException(kind + " TypeCode requires a repository ID");
    }
  }

  private static void requireEmpty(Optional<?> optional, String message) {
    if (optional.isPresent()) {
      throw new IllegalArgumentException(message);
    }
  }

  private static void requireEmpty(List<?> values, String message) {
    if (!values.isEmpty()) {
      throw new IllegalArgumentException(message);
    }
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
