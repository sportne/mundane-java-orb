package io.github.mundanej.mjo.rmi.iiop;

import java.util.Objects;

/**
 * Deterministic generated IDL text for one RMI Java-to-IDL model.
 *
 * @param sourceName stable logical source name used by parser and semantic tests
 * @param idlText generated IDL text with a trailing newline
 */
public record RmiGeneratedIdlFixture(String sourceName, String idlText) {

  /** Creates an immutable generated IDL fixture. */
  public RmiGeneratedIdlFixture {
    sourceName = requireNonBlank(sourceName, "sourceName");
    idlText = requireNonBlank(idlText, "idlText");
    if (!idlText.endsWith("\n")) {
      throw new IllegalArgumentException("idlText must end with a newline");
    }
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
