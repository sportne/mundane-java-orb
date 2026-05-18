package io.github.mundanej.mjo.interop.testkit;

/** Immutable peer identity used by interop reports and orchestration plans. */
public record InteropPeer(String name, String version) {
  public InteropPeer {
    requireNotBlank(name, "name");
    requireNotBlank(version, "version");
  }

  private static void requireNotBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }
}
