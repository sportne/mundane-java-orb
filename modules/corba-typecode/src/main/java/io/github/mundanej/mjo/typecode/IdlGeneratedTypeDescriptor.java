package io.github.mundanej.mjo.typecode;

import io.github.mundanej.mjo.repositoryid.RepositoryId;
import java.util.List;
import java.util.Objects;

/**
 * Static descriptor for one generated IDL declaration.
 *
 * @param kind generated IDL declaration category
 * @param idlScopedName absolute IDL scoped name
 * @param javaName mapped Java qualified name
 * @param repositoryId repository ID assigned to the declaration
 * @param fields struct or exception fields
 * @param enumConstants enum constants in IDL encounter order
 * @param operations interface operations in IDL encounter order
 */
public record IdlGeneratedTypeDescriptor(
    IdlTypeKind kind,
    String idlScopedName,
    String javaName,
    RepositoryId repositoryId,
    List<IdlFieldDescriptor> fields,
    List<String> enumConstants,
    List<IdlOperationDescriptor> operations) {

  /** Creates a validated generated type descriptor. */
  public IdlGeneratedTypeDescriptor {
    Objects.requireNonNull(kind, "kind");
    idlScopedName = requireNonBlank(idlScopedName, "idlScopedName");
    javaName = requireNonBlank(javaName, "javaName");
    Objects.requireNonNull(repositoryId, "repositoryId");
    fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    enumConstants = List.copyOf(Objects.requireNonNull(enumConstants, "enumConstants"));
    operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
