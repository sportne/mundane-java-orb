package io.github.mundanej.mjo.interop.testkit;

/** Immutable runtime label used in structured interop reports. */
public record InteropRuntime(String name) {
  public InteropRuntime {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
  }
}
