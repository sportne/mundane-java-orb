package io.github.mundanej.mjo.interop.testkit;

/** Immutable scenario identity and IDL corpus path for an interop run. */
public record InteropScenario(String name, String idlPath) {
  public InteropScenario {
    requireNotBlank(name, "name");
    requireNotBlank(idlPath, "idlPath");
  }

  private static void requireNotBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }
}
