package io.github.mundanej.mjo.rmi.iiop;

import java.util.List;
import java.util.Objects;

/**
 * Explicit Java remote-interface declaration supplied by source tooling or tests.
 *
 * @param binaryName Java binary name for the interface
 * @param remote whether the declaration explicitly extends or otherwise represents java.rmi.Remote
 * @param operations operation declarations in deterministic source order
 */
public record RmiJavaRemoteInterface(
    String binaryName, boolean remote, List<RmiJavaOperation> operations) {

  /** Creates an immutable remote-interface declaration. */
  public RmiJavaRemoteInterface {
    Objects.requireNonNull(binaryName, "binaryName");
    operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
  }
}
