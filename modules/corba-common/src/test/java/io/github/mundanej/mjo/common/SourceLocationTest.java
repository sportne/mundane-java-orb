package io.github.mundanej.mjo.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for source positions and spans. */
@Tag("unit")
final class SourceLocationTest {

  @Test
  void sourcePositionUsesOneBasedLineAndColumnAndZeroBasedOffset() {
    SourcePosition position = new SourcePosition("hello.idl", 1, 2, 0);

    assertEquals("hello.idl", position.sourceName());
    assertEquals(1, position.line());
    assertEquals(2, position.column());
    assertEquals(0, position.offset());
  }

  @Test
  void sourcePositionAcceptsMaximumOffset() {
    SourcePosition position = new SourcePosition("huge.idl", Integer.MAX_VALUE, 1, Long.MAX_VALUE);

    assertEquals(Integer.MAX_VALUE, position.line());
    assertEquals(Long.MAX_VALUE, position.offset());
  }

  @Test
  void sourcePositionRejectsInvalidCoordinates() {
    assertThrows(NullPointerException.class, () -> new SourcePosition(null, 1, 1, 0));
    assertThrows(IllegalArgumentException.class, () -> new SourcePosition(" ", 1, 1, 0));
    assertThrows(IllegalArgumentException.class, () -> new SourcePosition("hello.idl", 0, 1, 0));
    assertThrows(IllegalArgumentException.class, () -> new SourcePosition("hello.idl", 1, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new SourcePosition("hello.idl", 1, 1, -1));
  }

  @Test
  void sourceSpanRequiresSameSourceAndNondecreasingOffsets() {
    SourcePosition start = new SourcePosition("hello.idl", 1, 1, 0);
    SourcePosition end = new SourcePosition("hello.idl", 1, 5, 4);

    SourceSpan span = new SourceSpan(start, end);

    assertEquals(start, span.start());
    assertEquals(end, span.end());
  }

  @Test
  void sourceSpanRejectsDifferentSources() {
    SourcePosition start = new SourcePosition("a.idl", 1, 1, 0);
    SourcePosition end = new SourcePosition("b.idl", 1, 1, 0);

    assertThrows(IllegalArgumentException.class, () -> new SourceSpan(start, end));
    assertThrows(NullPointerException.class, () -> new SourceSpan(null, end));
    assertThrows(NullPointerException.class, () -> new SourceSpan(start, null));
  }

  @Test
  void sourceSpanRejectsDecreasingOffsets() {
    SourcePosition start = new SourcePosition("hello.idl", 1, 5, 4);
    SourcePosition end = new SourcePosition("hello.idl", 1, 1, 0);

    assertThrows(IllegalArgumentException.class, () -> new SourceSpan(start, end));
  }
}
