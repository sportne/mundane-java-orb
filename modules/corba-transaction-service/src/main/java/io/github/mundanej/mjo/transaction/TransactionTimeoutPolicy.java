package io.github.mundanej.mjo.transaction;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Explicit timeout policy for local Transaction Service coordinator entries. */
public record TransactionTimeoutPolicy(Duration defaultTimeout, Duration maxTimeout) {

  /** Default local transaction timeout. */
  public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

  /** Default upper bound accepted for a local transaction timeout. */
  public static final Duration DEFAULT_MAX_TIMEOUT = Duration.ofHours(1);

  /** Creates a validated timeout policy. */
  public TransactionTimeoutPolicy {
    defaultTimeout = requirePositive(defaultTimeout, "defaultTimeout");
    maxTimeout = requirePositive(maxTimeout, "maxTimeout");
    if (defaultTimeout.compareTo(maxTimeout) > 0) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.INVALID_TIMEOUT,
          "defaultTimeout must not exceed maxTimeout");
    }
  }

  /** Returns the default bounded local timeout policy. */
  public static TransactionTimeoutPolicy defaults() {
    return new TransactionTimeoutPolicy(DEFAULT_TIMEOUT, DEFAULT_MAX_TIMEOUT);
  }

  Instant deadlineFor(Instant beganAt, Duration requestedTimeout) {
    Objects.requireNonNull(beganAt, "beganAt");
    Duration timeout = requestedTimeout == null ? defaultTimeout : requestedTimeout;
    timeout = requirePositive(timeout, "timeout");
    if (timeout.compareTo(maxTimeout) > 0) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.INVALID_TIMEOUT,
          "timeout must not exceed " + maxTimeout);
    }
    try {
      return beganAt.plus(timeout);
    } catch (ArithmeticException | DateTimeException exception) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.INVALID_TIMEOUT, "timeout deadline overflow");
    }
  }

  private static Duration requirePositive(Duration value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isZero() || value.isNegative()) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.INVALID_TIMEOUT, name + " must be positive");
    }
    return value;
  }
}
