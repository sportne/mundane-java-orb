package io.github.mundanej.mjo.idl.java.mapping;

import java.util.List;
import java.util.Objects;

/**
 * Java type selected for an IDL declaration.
 *
 * @param kind generated Java type category
 * @param name Java type name
 * @param fields struct or exception fields
 * @param enumConstants enum constants
 * @param operations interface operations
 * @param attributes interface attributes
 * @param baseInterfaces inherited Java interface or valuetype names
 * @param supportedInterfaces supported Java interface names for valuetypes
 * @param aliasType mapped Java target type for typedefs and synthetic holders
 * @param repositoryId IDL repository ID for helper and descriptor metadata
 * @param abstractType whether the generated type should be abstract
 */
public record JavaMappedType(
    JavaMappedTypeKind kind,
    JavaMappedName name,
    List<JavaMappedField> fields,
    List<String> enumConstants,
    List<JavaMappedOperation> operations,
    List<JavaMappedAttribute> attributes,
    List<String> baseInterfaces,
    List<String> supportedInterfaces,
    String aliasType,
    String repositoryId,
    boolean abstractType) {

  /** Creates a validated mapped type. */
  public JavaMappedType {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(name, "name");
    fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    enumConstants = List.copyOf(Objects.requireNonNull(enumConstants, "enumConstants"));
    operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
    attributes = List.copyOf(Objects.requireNonNull(attributes, "attributes"));
    baseInterfaces = List.copyOf(Objects.requireNonNull(baseInterfaces, "baseInterfaces"));
    supportedInterfaces =
        List.copyOf(Objects.requireNonNull(supportedInterfaces, "supportedInterfaces"));
    Objects.requireNonNull(aliasType, "aliasType");
    Objects.requireNonNull(repositoryId, "repositoryId");
    if ((kind == JavaMappedTypeKind.TYPEDEF || kind == JavaMappedTypeKind.HOLDER)
        && aliasType.isBlank()) {
      throw new IllegalArgumentException("aliasType must not be blank for " + kind);
    }
  }

  /** Creates a mapped type without valuetype support or explicit repository metadata. */
  public JavaMappedType(
      JavaMappedTypeKind kind,
      JavaMappedName name,
      List<JavaMappedField> fields,
      List<String> enumConstants,
      List<JavaMappedOperation> operations,
      List<JavaMappedAttribute> attributes,
      List<String> baseInterfaces,
      String aliasType) {
    this(
        kind,
        name,
        fields,
        enumConstants,
        operations,
        attributes,
        baseInterfaces,
        List.of(),
        aliasType,
        name.qualifiedName(),
        false);
  }

  /** Creates a mapped type without G10 inheritance or alias metadata. */
  public JavaMappedType(
      JavaMappedTypeKind kind,
      JavaMappedName name,
      List<JavaMappedField> fields,
      List<String> enumConstants,
      List<JavaMappedOperation> operations,
      List<JavaMappedAttribute> attributes) {
    this(kind, name, fields, enumConstants, operations, attributes, List.of(), "");
  }
}
