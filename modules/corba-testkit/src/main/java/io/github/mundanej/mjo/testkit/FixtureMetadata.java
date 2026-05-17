package io.github.mundanej.mjo.testkit;

import java.util.Objects;

/**
 * Validated metadata for a reusable fixture.
 *
 * @param id stable fixture identifier
 * @param kind fixture category
 * @param relativePath safe fixture path relative to a fixture root
 * @param specReference specification reference or verification scope
 */
public record FixtureMetadata(
    String id, FixtureKind kind, String relativePath, String specReference) {

  /** Creates validated fixture metadata. */
  public FixtureMetadata {
    id = requireNonBlank(id, "id");
    Objects.requireNonNull(kind, "kind");
    relativePath = FixtureSet.requireSafeRelativePath(relativePath).toString().replace('\\', '/');
    specReference = requireNonBlank(specReference, "specReference");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
