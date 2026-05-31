package io.github.mundanej.mjo.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Tests for bounded restart bind retry helpers. */
final class RestartBindRetryTest {

  @Test
  void runRetriesUntilActionSucceeds() throws Exception {
    AtomicInteger attempts = new AtomicInteger();

    RestartBindRetry.run(
        "restart failed",
        3,
        Duration.ZERO,
        () -> {
          if (attempts.incrementAndGet() < 3) {
            throw new RetryableBindFailure();
          }
        },
        RetryableBindFailure.class::isInstance);

    assertEquals(3, attempts.get());
  }

  @Test
  void getReturnsSuccessfulValueAfterRetry() throws Exception {
    AtomicInteger attempts = new AtomicInteger();

    String value =
        RestartBindRetry.get(
            "restart failed",
            2,
            Duration.ZERO,
            () -> {
              if (attempts.incrementAndGet() == 1) {
                throw new RetryableBindFailure();
              }
              return "bound";
            },
            RetryableBindFailure.class::isInstance);

    assertEquals("bound", value);
    assertEquals(2, attempts.get());
  }

  @Test
  void nonRetryableFailurePropagatesImmediately() {
    IllegalStateException failure = new IllegalStateException("wrong failure");

    IllegalStateException thrown =
        assertThrows(
            IllegalStateException.class,
            () ->
                RestartBindRetry.run(
                    "restart failed",
                    3,
                    Duration.ZERO,
                    () -> {
                      throw failure;
                    },
                    RetryableBindFailure.class::isInstance));

    assertSame(failure, thrown);
  }

  @Test
  void exhaustedRetryableFailuresReportLastCause() {
    AssertionError failure =
        assertThrows(
            AssertionError.class,
            () ->
                RestartBindRetry.run(
                    "restart failed",
                    2,
                    Duration.ZERO,
                    () -> {
                      throw new RetryableBindFailure();
                    },
                    RetryableBindFailure.class::isInstance));

    assertEquals("restart failed", failure.getMessage());
    assertEquals(RetryableBindFailure.class, failure.getCause().getClass());
  }

  @Test
  void rejectsInvalidRetryConfiguration() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            RestartBindRetry.run(
                "restart failed",
                0,
                Duration.ZERO,
                () -> {},
                RetryableBindFailure.class::isInstance));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            RestartBindRetry.run(
                "restart failed",
                1,
                Duration.ofMillis(-1),
                () -> {},
                RetryableBindFailure.class::isInstance));
  }

  private static final class RetryableBindFailure extends Exception {

    private static final long serialVersionUID = 1L;
  }
}
