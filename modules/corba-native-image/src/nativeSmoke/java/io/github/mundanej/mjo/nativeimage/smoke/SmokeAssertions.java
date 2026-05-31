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

  static <T extends Throwable> void requireThrows(
      Class<T> expected, ThrowingAction action, String label) {
    try {
      action.run();
    } catch (Throwable thrown) {
      if (expected.isInstance(thrown)) {
        return;
      }
      throw new AssertionError(
          "Native Image smoke failed: "
              + label
              + "; expected "
              + expected.getName()
              + ", got "
              + thrown.getClass().getName(),
          thrown);
    }
    throw new AssertionError(
        "Native Image smoke failed: " + label + "; expected " + expected.getName());
  }

  @FunctionalInterface
  interface ThrowingAction {
    void run() throws Exception;
  }
}
