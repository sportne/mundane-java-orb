package io.github.mundanej.mjo.typecode;

import io.github.mundanej.mjo.repositoryid.RepositoryId;
import java.util.Objects;
import java.util.Optional;

/**
 * Static reference to an IDL type as seen by generated descriptors.
 *
 * @param kind type category
 * @param idlName IDL keyword or absolute scoped name
 * @param javaName mapped Java type spelling
 * @param repositoryId repository ID for user-defined IDL types
 */
public record IdlTypeReference(
    IdlTypeKind kind, String idlName, String javaName, Optional<RepositoryId> repositoryId) {

  /** Creates a validated type reference. */
  public IdlTypeReference {
    Objects.requireNonNull(kind, "kind");
    idlName = requireNonBlank(idlName, "idlName");
    javaName = requireNonBlank(javaName, "javaName");
    Objects.requireNonNull(repositoryId, "repositoryId");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
