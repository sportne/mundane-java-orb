package io.github.mundanej.mjo.time;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/** Local TimeService subset for TimeBase value creation and universal time queries. */
public final class LocalTimeService {

  /** TimeBase time unit: 100 nanoseconds. */
  public static final long TICKS_PER_SECOND = 10_000_000L;

  /** TimeBase Gregorian epoch for universal time ticks. */
  public static final Instant GREGORIAN_EPOCH = Instant.parse("1582-10-15T00:00:00Z");

  private static final long NANOS_PER_TICK = 100L;

  private final TimeServiceOptions options;

  private LocalTimeService(TimeServiceOptions options) {
    this.options = Objects.requireNonNull(options, "options");
  }

  /** Creates a local Time Service with caller-provided clock policy. */
  public static LocalTimeService create(TimeServiceOptions options) {
    return new LocalTimeService(options);
  }

  /** Creates a local Time Service using the system UTC clock. */
  public static LocalTimeService systemUtc() {
    return create(TimeServiceOptions.systemUtc());
  }

  /** Returns universal time from the configured clock. */
  public UtcTime universalTime() {
    Instant now;
    try {
      now = options.clock().instant();
    } catch (RuntimeException exception) {
      throw new TimeServiceException(
          TimeServiceDiagnosticCodes.CLOCK_UNAVAILABLE,
          "configured clock did not provide universal time",
          exception);
    }
    return newUniversalTime(
        timeTicks(now), inaccuracyTicks(options.inaccuracy()), offsetMinutes(options.offset()));
  }

  /** Creates a validated universal time value from TimeBase-compatible fields. */
  public UtcTime newUniversalTime(long timeTicks, long inaccuracyTicks, short tdfMinutes) {
    return new UtcTime(timeTicks, inaccuracyTicks, tdfMinutes);
  }

  /** Creates a validated interval value from TimeBase-compatible fields. */
  public TimeInterval newInterval(long lowerTicks, long upperTicks) {
    return new TimeInterval(lowerTicks, upperTicks);
  }

  static long timeTicks(Instant instant) {
    Objects.requireNonNull(instant, "instant");
    if (instant.isBefore(GREGORIAN_EPOCH)) {
      throw new TimeServiceException(
          TimeServiceDiagnosticCodes.INVALID_TIME,
          "instant must not be before the TimeBase Gregorian epoch");
    }
    Duration elapsed = Duration.between(GREGORIAN_EPOCH, instant);
    try {
      return Math.addExact(
          Math.multiplyExact(elapsed.getSeconds(), TICKS_PER_SECOND),
          elapsed.getNano() / NANOS_PER_TICK);
    } catch (ArithmeticException exception) {
      throw new TimeServiceException(
          TimeServiceDiagnosticCodes.INVALID_TIME,
          "instant exceeds supported TimeBase tick range",
          exception);
    }
  }

  static long inaccuracyTicks(Duration duration) {
    Objects.requireNonNull(duration, "duration");
    if (duration.isNegative()) {
      throw new TimeServiceException(
          TimeServiceDiagnosticCodes.INVALID_INACCURACY, "inaccuracy must be nonnegative");
    }
    long ticks;
    try {
      ticks =
          Math.addExact(
              Math.multiplyExact(duration.getSeconds(), TICKS_PER_SECOND),
              ceilNanosToTicks(duration.getNano()));
    } catch (ArithmeticException exception) {
      throw new TimeServiceException(
          TimeServiceDiagnosticCodes.INVALID_INACCURACY,
          "inaccuracy exceeds supported TimeBase tick range",
          exception);
    }
    if (ticks > UtcTime.MAX_INACCURACY_TICKS) {
      throw new TimeServiceException(
          TimeServiceDiagnosticCodes.INVALID_INACCURACY,
          "inaccuracy exceeds TimeBase 48-bit inaccuracy");
    }
    return ticks;
  }

  static short offsetMinutes(ZoneOffset offset) {
    Objects.requireNonNull(offset, "offset");
    int seconds = offset.getTotalSeconds();
    if (seconds % 60 != 0) {
      throw new TimeServiceException(
          TimeServiceDiagnosticCodes.INVALID_TDF, "offset must be representable in minutes");
    }
    return (short) (seconds / 60);
  }

  private static long ceilNanosToTicks(int nanos) {
    long ticks = nanos / NANOS_PER_TICK;
    return nanos % NANOS_PER_TICK == 0 ? ticks : ticks + 1L;
  }
}
