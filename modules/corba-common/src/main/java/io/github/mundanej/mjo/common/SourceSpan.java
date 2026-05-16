package io.github.mundanej.mjo.common;

import java.util.Objects;

/**
 * Inclusive source span between two positions in the same source.
 *
 * @param start first position in the span
 * @param end final position in the span
 */
public record SourceSpan(SourcePosition start, SourcePosition end) {

  /** Creates a validated source span. */
  public SourceSpan {
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    if (!start.sourceName().equals(end.sourceName())) {
      throw new IllegalArgumentException("Source span positions must use the same source");
    }
    if (end.offset() < start.offset()) {
      throw new IllegalArgumentException("Source span end offset must not precede start offset");
    }
  }
}
