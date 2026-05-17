package io.github.mundanej.mjo.idl.preprocessor;

import java.util.Objects;

/**
 * Source text supplied to the IDL preprocessor.
 *
 * @param sourceName stable source identifier used in diagnostics and token spans
 * @param sourceText source text as Java {@link String} characters
 */
public record IdlSource(String sourceName, String sourceText) {

  /** Creates a validated source value. */
  public IdlSource {
    sourceName = requireNonBlank(sourceName, "sourceName");
    Objects.requireNonNull(sourceText, "sourceText");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
