package io.github.mundanej.mjo.orb;

import java.util.Objects;

/** Thrown when local ORB lifecycle, registry, or invocation validation fails. */
public final class LocalOrbException extends IllegalStateException {

  private static final long serialVersionUID = 1L;

  /** Creates a local ORB exception. */
  public LocalOrbException(String message) {
    super(requireNonBlank(message, "message"));
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
