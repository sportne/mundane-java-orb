package io.github.mundanej.mjo.idl.java.mapping;

import java.util.List;
import java.util.Objects;

/**
 * Deterministic Java mapping model derived from one valid IDL semantic model.
 *
 * @param mode mapping mode
 * @param sourceName source IDL identity used for generated documentation
 * @param types generated Java types in IDL encounter order
 * @param constantScopes generated constant holders in IDL encounter order
 */
public record JavaMappingModel(
    JavaMappingMode mode,
    String sourceName,
    List<JavaMappedType> types,
    List<JavaMappedConstantScope> constantScopes) {

  /** Creates a validated mapping model. */
  public JavaMappingModel {
    Objects.requireNonNull(mode, "mode");
    sourceName = requireNonBlank(sourceName, "sourceName");
    types = List.copyOf(Objects.requireNonNull(types, "types"));
    constantScopes = List.copyOf(Objects.requireNonNull(constantScopes, "constantScopes"));
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
