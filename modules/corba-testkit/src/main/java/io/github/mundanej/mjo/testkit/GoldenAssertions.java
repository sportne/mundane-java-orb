package io.github.mundanej.mjo.testkit;

import java.util.Arrays;
import java.util.Objects;

/** Assertion helpers for golden fixture comparisons without a main-scope test dependency. */
public final class GoldenAssertions {

  private GoldenAssertions() {}

  /** Asserts normalized golden text equality. */
  public static void assertTextEquals(String label, String expected, String actual) {
    Objects.requireNonNull(label, "label");
    String normalizedExpected = TextFixtureNormalizer.normalize(expected);
    String normalizedActual = TextFixtureNormalizer.normalize(actual);
    if (!normalizedExpected.equals(normalizedActual)) {
      throw new AssertionError(textMismatchMessage(label, normalizedExpected, normalizedActual));
    }
  }

  /** Asserts golden byte equality. */
  public static void assertBytesEquals(String label, byte[] expected, byte[] actual) {
    Objects.requireNonNull(label, "label");
    Objects.requireNonNull(expected, "expected");
    Objects.requireNonNull(actual, "actual");
    if (!Arrays.equals(expected, actual)) {
      throw new AssertionError(byteMismatchMessage(label, expected, actual));
    }
  }

  private static String textMismatchMessage(String label, String expected, String actual) {
    int difference = firstTextDifference(expected, actual);
    return "Golden text mismatch for "
        + label
        + " at index "
        + difference
        + "; expected length "
        + expected.length()
        + ", actual length "
        + actual.length()
        + "; expected "
        + describeCharAt(expected, difference)
        + ", actual "
        + describeCharAt(actual, difference);
  }

  private static int firstTextDifference(String expected, String actual) {
    int commonLength = Math.min(expected.length(), actual.length());
    for (int index = 0; index < commonLength; index++) {
      if (expected.charAt(index) != actual.charAt(index)) {
        return index;
      }
    }
    return commonLength;
  }

  private static String describeCharAt(String value, int index) {
    if (index >= value.length()) {
      return "<end>";
    }
    char character = value.charAt(index);
    return "U+" + String.format("%04X", (int) character) + " '" + printable(character) + "'";
  }

  private static String printable(char character) {
    return switch (character) {
      case '\n' -> "\\n";
      case '\r' -> "\\r";
      case '\t' -> "\\t";
      default -> Character.toString(character);
    };
  }

  private static String byteMismatchMessage(String label, byte[] expected, byte[] actual) {
    int difference = firstByteDifference(expected, actual);
    return "Golden byte mismatch for "
        + label
        + " at offset "
        + difference
        + "; expected length "
        + expected.length
        + ", actual length "
        + actual.length
        + "; expected "
        + describeByteAt(expected, difference)
        + ", actual "
        + describeByteAt(actual, difference);
  }

  private static int firstByteDifference(byte[] expected, byte[] actual) {
    int commonLength = Math.min(expected.length, actual.length);
    for (int index = 0; index < commonLength; index++) {
      if (expected[index] != actual[index]) {
        return index;
      }
    }
    return commonLength;
  }

  private static String describeByteAt(byte[] value, int index) {
    if (index >= value.length) {
      return "<end>";
    }
    return String.format("0x%02X", value[index] & 0xFF);
  }
}
