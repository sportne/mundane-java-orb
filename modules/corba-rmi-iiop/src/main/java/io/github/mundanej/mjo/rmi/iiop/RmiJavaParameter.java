package io.github.mundanej.mjo.rmi.iiop;

import java.util.Objects;

/**
 * Explicit Java remote-interface parameter declaration.
 *
 * @param name Java parameter name
 * @param type declared Java parameter type
 */
public record RmiJavaParameter(String name, RmiJavaTypeReference type) {

  /** Creates an immutable parameter declaration. */
  public RmiJavaParameter {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
  }
}
