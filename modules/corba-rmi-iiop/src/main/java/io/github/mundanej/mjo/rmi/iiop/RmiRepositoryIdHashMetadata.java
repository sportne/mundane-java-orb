package io.github.mundanej.mjo.rmi.iiop;

import java.util.Objects;
import java.util.Optional;

/**
 * Explicit RMI repository ID hash metadata supplied by build tooling or tests.
 *
 * @param javaBinaryName Java binary class or interface name
 * @param hash explicit 16-hex-digit RMI repository hash
 * @param serialVersionUid optional explicit 16-hex-digit serialVersionUID
 */
public record RmiRepositoryIdHashMetadata(
    String javaBinaryName, String hash, Optional<String> serialVersionUid) {

  /** Creates immutable hash metadata. */
  public RmiRepositoryIdHashMetadata {
    Objects.requireNonNull(javaBinaryName, "javaBinaryName");
    Objects.requireNonNull(hash, "hash");
    Objects.requireNonNull(serialVersionUid, "serialVersionUid");
  }

  /** Creates hash metadata without a serialVersionUID component. */
  public RmiRepositoryIdHashMetadata(String javaBinaryName, String hash) {
    this(javaBinaryName, hash, Optional.empty());
  }

  /** Creates hash metadata with a serialVersionUID component. */
  public RmiRepositoryIdHashMetadata(String javaBinaryName, String hash, String serialVersionUid) {
    this(javaBinaryName, hash, Optional.of(serialVersionUid));
  }
}
