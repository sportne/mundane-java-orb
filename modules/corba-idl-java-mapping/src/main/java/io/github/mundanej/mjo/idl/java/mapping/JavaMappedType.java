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
 */
public record JavaMappedType(
    JavaMappedTypeKind kind,
    JavaMappedName name,
    List<JavaMappedField> fields,
    List<String> enumConstants,
    List<JavaMappedOperation> operations,
    List<JavaMappedAttribute> attributes) {

  /** Creates a validated mapped type. */
  public JavaMappedType {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(name, "name");
    fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    enumConstants = List.copyOf(Objects.requireNonNull(enumConstants, "enumConstants"));
    operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
    attributes = List.copyOf(Objects.requireNonNull(attributes, "attributes"));
  }
}
