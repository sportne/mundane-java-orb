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
 * @param baseInterfaces inherited Java interface names
 * @param aliasType mapped Java target type for typedefs and synthetic holders
 */
public record JavaMappedType(
    JavaMappedTypeKind kind,
    JavaMappedName name,
    List<JavaMappedField> fields,
    List<String> enumConstants,
    List<JavaMappedOperation> operations,
    List<JavaMappedAttribute> attributes,
    List<String> baseInterfaces,
    String aliasType) {

  /** Creates a validated mapped type. */
  public JavaMappedType {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(name, "name");
    fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    enumConstants = List.copyOf(Objects.requireNonNull(enumConstants, "enumConstants"));
    operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
    attributes = List.copyOf(Objects.requireNonNull(attributes, "attributes"));
    baseInterfaces = List.copyOf(Objects.requireNonNull(baseInterfaces, "baseInterfaces"));
    Objects.requireNonNull(aliasType, "aliasType");
    if ((kind == JavaMappedTypeKind.TYPEDEF || kind == JavaMappedTypeKind.HOLDER)
        && aliasType.isBlank()) {
      throw new IllegalArgumentException("aliasType must not be blank for " + kind);
    }
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
