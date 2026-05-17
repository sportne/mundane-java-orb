package io.github.mundanej.mjo.idl.java.mapping;

import java.util.List;
import java.util.Objects;

/**
 * Deterministic generated holder for constants declared in one IDL scope.
 *
 * @param name Java holder type name
 * @param constants constants in IDL encounter order
 */
public record JavaMappedConstantScope(JavaMappedName name, List<JavaMappedConstant> constants) {

  /** Creates a validated constant-scope mapping. */
  public JavaMappedConstantScope {
    Objects.requireNonNull(name, "name");
    constants = List.copyOf(Objects.requireNonNull(constants, "constants"));
    if (constants.isEmpty()) {
      throw new IllegalArgumentException("constants must not be empty");
    }
  }
}
