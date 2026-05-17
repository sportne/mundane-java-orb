package io.github.mundanej.mjo.idl.ast;

import java.util.Objects;

final class IdlAstValidation {

  private IdlAstValidation() {}

  static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
