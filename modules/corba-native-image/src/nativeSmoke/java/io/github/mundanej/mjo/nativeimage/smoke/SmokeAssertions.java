package io.github.mundanej.mjo.nativeimage.smoke;

final class SmokeAssertions {

  private SmokeAssertions() {}

  static void require(boolean condition, String label) {
    if (!condition) {
      throw new AssertionError("Native Image smoke failed: " + label);
    }
  }

  static void requireEquals(Object expected, Object actual, String label) {
    if (!expected.equals(actual)) {
      throw new AssertionError(
          "Native Image smoke failed: " + label + "; expected " + expected + ", got " + actual);
    }
  }
}
