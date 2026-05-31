package io.github.mundanej.mjo.testkit;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;

/** Bounded retry helper for restart tests that must rebind a recently closed endpoint. */
public final class RestartBindRetry {

  private RestartBindRetry() {}

  /** Runs a checked action until it succeeds or retryable failures are exhausted. */
  public static void run(
      String failureMessage,
      int maxAttempts,
      Duration retryDelay,
      CheckedRunnable action,
      Predicate<Throwable> retryableFailure)
      throws Exception {
    Objects.requireNonNull(action, "action");
    get(
        failureMessage,
        maxAttempts,
        retryDelay,
        () -> {
          action.run();
          return null;
        },
        retryableFailure);
  }

  /** Gets a value from a checked supplier until it succeeds or retryable failures are exhausted. */
  public static <T> T get(
      String failureMessage,
      int maxAttempts,
      Duration retryDelay,
      CheckedSupplier<T> supplier,
      Predicate<Throwable> retryableFailure)
      throws Exception {
    requirePositiveAttempts(maxAttempts);
    long retryDelayMillis = requireRetryDelay(retryDelay).toMillis();
    Objects.requireNonNull(supplier, "supplier");
    Objects.requireNonNull(retryableFailure, "retryableFailure");

    Throwable lastRetryableFailure = null;
    for (int attempt = 0; attempt < maxAttempts; attempt++) {
      try {
        return supplier.get();
      } catch (Exception exception) {
        if (!retryableFailure.test(exception)) {
          throw exception;
        }
        lastRetryableFailure = exception;
      } catch (AssertionError error) {
        if (!retryableFailure.test(error)) {
          throw error;
        }
        lastRetryableFailure = error;
      }
      sleepBeforeNextAttempt(attempt, maxAttempts, retryDelayMillis);
    }
    throw new AssertionError(failureMessage, lastRetryableFailure);
  }

  private static void requirePositiveAttempts(int maxAttempts) {
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be positive");
    }
  }

  private static Duration requireRetryDelay(Duration retryDelay) {
    Objects.requireNonNull(retryDelay, "retryDelay");
    if (retryDelay.isNegative()) {
      throw new IllegalArgumentException("retryDelay must not be negative");
    }
    return retryDelay;
  }

  private static void sleepBeforeNextAttempt(int attempt, int maxAttempts, long retryDelayMillis)
      throws InterruptedException {
    if (attempt + 1 < maxAttempts && retryDelayMillis > 0L) {
      Thread.sleep(retryDelayMillis);
    }
  }

  /** Checked action used by restart retry tests. */
  @FunctionalInterface
  public interface CheckedRunnable {

    /** Runs the action. */
    void run() throws Exception;
  }

  /** Checked supplier used by restart retry tests. */
  @FunctionalInterface
  public interface CheckedSupplier<T> {

    /** Gets the value. */
    T get() throws Exception;
  }
}
