package io.github.mundanej.mjo.rmi.iiop;

import java.util.Objects;

/**
 * Decoded local RMI-IIOP user exception payload for the approved empty-exception slice.
 *
 * @param exception declared exception reference
 * @param repositoryId RMI repository ID read from or written to the CDR payload
 */
public record RmiCdrUserExceptionPayload(RmiIdlExceptionReference exception, String repositoryId) {

  /** Creates an immutable user exception payload descriptor. */
  public RmiCdrUserExceptionPayload {
    Objects.requireNonNull(exception, "exception");
    repositoryId = requireNonBlank(repositoryId, "repositoryId");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
