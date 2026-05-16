package io.github.mundanej.mjo.common;

import java.util.Objects;

/**
 * Position in a source document.
 *
 * @param sourceName source identifier or path as supplied by the caller
 * @param line one-based line number
 * @param column one-based column number
 * @param offset zero-based character offset
 */
public record SourcePosition(String sourceName, int line, int column, long offset) {

  /** Creates a validated source position. */
  public SourcePosition {
    sourceName = requireNonBlank(sourceName, "sourceName");
    if (line < 1) {
      throw new IllegalArgumentException("line must be one-based");
    }
    if (column < 1) {
      throw new IllegalArgumentException("column must be one-based");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("offset must be zero-based");
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
