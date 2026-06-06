package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.time.LocalTimeService;
import io.github.mundanej.mjo.time.TimeInterval;
import io.github.mundanej.mjo.time.TimeServiceDiagnosticCodes;
import io.github.mundanej.mjo.time.TimeServiceException;
import io.github.mundanej.mjo.time.TimeServiceOptions;
import io.github.mundanej.mjo.time.UtcTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

/** Native Image smoke coverage for the supported local Time Service slice. */
public final class TimeServiceNativeSmoke {

  private TimeServiceNativeSmoke() {}

  /** Runs the Time Service Native Image smoke checks. */
  public static void main(String[] args) {
    LocalTimeService service =
        LocalTimeService.create(
            new TimeServiceOptions(
                Clock.fixed(Instant.parse("1582-10-15T00:00:02.000000101Z"), ZoneOffset.UTC),
                Duration.ofNanos(101),
                ZoneOffset.UTC));

    UtcTime current = service.universalTime();
    SmokeAssertions.requireEquals(20_000_001L, current.timeTicks(), "time ticks");
    SmokeAssertions.requireEquals(2L, current.inaccuracyTicks(), "inaccuracy ticks");
    SmokeAssertions.requireEquals((short) 0, current.tdfMinutes(), "tdf minutes");

    UtcTime explicit = service.newUniversalTime(10L, 20L, (short) -60);
    SmokeAssertions.requireEquals(10L, explicit.timeTicks(), "explicit time ticks");
    SmokeAssertions.requireEquals(20L, explicit.inaccuracyTicks(), "explicit inaccuracy");
    SmokeAssertions.requireEquals((short) -60, explicit.tdfMinutes(), "explicit tdf");

    TimeInterval interval = service.newInterval(3L, 5L);
    SmokeAssertions.requireEquals(3L, interval.lowerBoundTicks(), "interval lower");
    SmokeAssertions.requireEquals(5L, interval.upperBoundTicks(), "interval upper");

    try {
      service.newUniversalTime(1L, UtcTime.MAX_INACCURACY_TICKS + 1L, (short) 0);
      throw new AssertionError("invalid inaccuracy was accepted");
    } catch (TimeServiceException expected) {
      SmokeAssertions.requireEquals(
          TimeServiceDiagnosticCodes.INVALID_INACCURACY,
          expected.code(),
          "invalid inaccuracy diagnostic");
    }
  }
}
