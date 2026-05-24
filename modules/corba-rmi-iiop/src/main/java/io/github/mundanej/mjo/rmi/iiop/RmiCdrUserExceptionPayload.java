package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;

/**
 * Decoded local RMI-IIOP user exception payload for the approved empty-exception slice.
 *
 * @param exception declared exception reference
 * @param repositoryId RMI repository ID read from or written to the CDR payload
 * @param fields decoded user-exception field values
 */
public record RmiCdrUserExceptionPayload(
    RmiIdlExceptionReference exception, String repositoryId, List<RmiCdrValue> fields) {

  /** Creates an immutable user exception payload descriptor. */
  public RmiCdrUserExceptionPayload {
    Objects.requireNonNull(exception, "exception");
    repositoryId = requireNonBlank(repositoryId, "repositoryId");
    fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
  }

  /** Creates an empty user-exception payload descriptor. */
  public RmiCdrUserExceptionPayload(RmiIdlExceptionReference exception, String repositoryId) {
    this(exception, repositoryId, List.of());
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
