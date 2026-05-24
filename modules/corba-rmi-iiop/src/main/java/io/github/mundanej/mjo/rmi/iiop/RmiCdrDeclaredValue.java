package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;

/**
 * Explicit local declared-value payload for the bounded RMI-IIOP CDR codec.
 *
 * @param repositoryId declared value repository ID
 * @param members member values in declared IDL order
 */
public record RmiCdrDeclaredValue(String repositoryId, List<RmiCdrValue> members) {

  /** Creates an immutable declared-value payload. */
  public RmiCdrDeclaredValue {
    repositoryId = requireNonBlank(repositoryId, "repositoryId");
    members = List.copyOf(Objects.requireNonNull(members, "members"));
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
