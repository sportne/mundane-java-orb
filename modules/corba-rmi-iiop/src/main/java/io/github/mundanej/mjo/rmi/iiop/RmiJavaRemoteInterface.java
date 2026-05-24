package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;

/**
 * Explicit Java remote-interface declaration supplied by source tooling or tests.
 *
 * @param binaryName Java binary name for the interface
 * @param remote whether the declaration explicitly extends or otherwise represents java.rmi.Remote
 * @param operations operation declarations in deterministic source order
 * @param baseInterfaces remote base interfaces in deterministic declaration order
 */
public record RmiJavaRemoteInterface(
    String binaryName,
    boolean remote,
    List<RmiJavaOperation> operations,
    List<RmiJavaTypeReference> baseInterfaces) {

  /** Creates an immutable remote-interface declaration. */
  public RmiJavaRemoteInterface {
    Objects.requireNonNull(binaryName, "binaryName");
    operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
    baseInterfaces = List.copyOf(Objects.requireNonNull(baseInterfaces, "baseInterfaces"));
  }

  /** Creates a remote-interface declaration without inherited interfaces. */
  public RmiJavaRemoteInterface(
      String binaryName, boolean remote, List<RmiJavaOperation> operations) {
    this(binaryName, remote, operations, List.of());
  }
}
