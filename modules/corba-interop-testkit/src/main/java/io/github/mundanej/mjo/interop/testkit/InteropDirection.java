package io.github.mundanej.mjo.interop.testkit;

import java.util.Objects;

/** Immutable client-to-server direction for one interop scenario execution. */
public record InteropDirection(InteropRuntime clientRuntime, InteropRuntime serverRuntime) {
  public InteropDirection {
    Objects.requireNonNull(clientRuntime, "clientRuntime");
    Objects.requireNonNull(serverRuntime, "serverRuntime");
  }
}
