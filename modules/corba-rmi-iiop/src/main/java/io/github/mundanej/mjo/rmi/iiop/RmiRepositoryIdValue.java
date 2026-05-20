package io.github.mundanej.mjo.rmi.iiop;

import java.util.Objects;

/**
 * Planned RMI repository ID string for one Java binary name.
 *
 * @param javaBinaryName Java binary class or interface name
 * @param repositoryId deterministic {@code RMI:<name>:<hash>[:<uid>]} value
 */
public record RmiRepositoryIdValue(String javaBinaryName, String repositoryId) {

  /** Creates an immutable planned repository ID value. */
  public RmiRepositoryIdValue {
    javaBinaryName = requireNonBlank(javaBinaryName, "javaBinaryName");
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
