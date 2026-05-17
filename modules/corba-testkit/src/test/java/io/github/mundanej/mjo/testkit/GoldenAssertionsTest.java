package io.github.mundanej.mjo.testkit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link GoldenAssertions}. */
@Tag("unit")
final class GoldenAssertionsTest {

  @Test
  void acceptsEqualTextAfterNormalization() {
    assertDoesNotThrow(
        () ->
            GoldenAssertions.assertTextEquals(
                "hello.idl", "\ufeffmodule hello {}\r\n", "module hello {}\n"));
  }

  @Test
  void reportsTextMismatchAtFirstDifferingIndex() {
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () -> GoldenAssertions.assertTextEquals("hello.idl", "abc\n", "abx\n"));

    assertEquals(
        "Golden text mismatch for hello.idl at index 2; expected length 4, actual length 4; "
            + "expected U+0063 'c', actual U+0078 'x'",
        error.getMessage());
  }

  @Test
  void reportsTextLengthMismatchAtEndOfCommonPrefix() {
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () -> GoldenAssertions.assertTextEquals("short.idl", "abc", "abcd"));

    assertTrue(error.getMessage().contains("at index 3"));
    assertTrue(error.getMessage().contains("expected <end>, actual U+0064 'd'"));
  }

  @Test
  void acceptsEqualBytes() {
    assertDoesNotThrow(
        () ->
            GoldenAssertions.assertBytesEquals(
                "message.bin", new byte[] {0x01, 0x02}, new byte[] {0x01, 0x02}));
  }

  @Test
  void reportsByteMismatchAtFirstDifferingOffset() {
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () ->
                GoldenAssertions.assertBytesEquals(
                    "message.bin", new byte[] {0x01, 0x02}, new byte[] {0x01, 0x03}));

    assertEquals(
        "Golden byte mismatch for message.bin at offset 1; expected length 2, actual length 2; "
            + "expected 0x02, actual 0x03",
        error.getMessage());
  }

  @Test
  void reportsByteLengthMismatchAtEndOfCommonPrefix() {
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () ->
                GoldenAssertions.assertBytesEquals(
                    "truncated.bin", new byte[] {0x01, 0x02}, new byte[] {0x01}));

    assertTrue(error.getMessage().contains("at offset 1"));
    assertTrue(error.getMessage().contains("expected 0x02, actual <end>"));
  }

  @Test
  void rejectsNullInputs() {
    assertThrows(
        NullPointerException.class, () -> GoldenAssertions.assertTextEquals(null, "a", "a"));
    assertThrows(
        NullPointerException.class, () -> GoldenAssertions.assertTextEquals("id", null, "a"));
    assertThrows(
        NullPointerException.class, () -> GoldenAssertions.assertTextEquals("id", "a", null));
    assertThrows(
        NullPointerException.class,
        () -> GoldenAssertions.assertBytesEquals(null, new byte[0], new byte[0]));
    assertThrows(
        NullPointerException.class,
        () -> GoldenAssertions.assertBytesEquals("id", null, new byte[0]));
    assertThrows(
        NullPointerException.class,
        () -> GoldenAssertions.assertBytesEquals("id", new byte[0], null));
  }
}
