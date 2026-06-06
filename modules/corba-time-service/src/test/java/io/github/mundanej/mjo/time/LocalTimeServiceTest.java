package io.github.mundanej.mjo.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for the supported local Time Service slice. */
@Tag("unit")
final class LocalTimeServiceTest {

  @Test
  void universalTimeUsesFixedClockWithTimeBaseTicksAndCeiledInaccuracy() {
    Clock clock =
        Clock.fixed(
            Instant.parse("1582-10-15T00:00:01.000000250Z"), ZoneOffset.ofHoursMinutes(5, 30));
    LocalTimeService service =
        LocalTimeService.create(
            new TimeServiceOptions(clock, Duration.ofNanos(101), ZoneOffset.ofHoursMinutes(5, 30)));

    UtcTime time = service.universalTime();

    assertEquals(10_000_002L, time.timeTicks());
    assertEquals(2L, time.inaccuracyTicks());
    assertEquals((short) 330, time.tdfMinutes());
  }

  @Test
  void instantBeforeGregorianEpochIsRejected() {
    TimeServiceException exception =
        assertThrows(
            TimeServiceException.class,
            () -> LocalTimeService.timeTicks(Instant.parse("1582-10-14T23:59:59.999999999Z")));

    assertEquals(TimeServiceDiagnosticCodes.INVALID_TIME, exception.code());
  }

  @Test
  void universalTimeReportsUnavailableClockDeterministically() {
    Clock failingClock =
        new Clock() {
          @Override
          public ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          @Override
          public Clock withZone(ZoneId zone) {
            return this;
          }

          @Override
          public Instant instant() {
            throw new IllegalStateException("clock offline");
          }
        };
    LocalTimeService service =
        LocalTimeService.create(
            new TimeServiceOptions(failingClock, Duration.ZERO, ZoneOffset.UTC));

    TimeServiceException exception =
        assertThrows(TimeServiceException.class, service::universalTime);

    assertEquals(TimeServiceDiagnosticCodes.CLOCK_UNAVAILABLE, exception.code());
    assertEquals(IllegalStateException.class, exception.getCause().getClass());
  }

  @Test
  void createsExplicitUniversalTimeAndRejectsInvalidInaccuracy() {
    LocalTimeService service = LocalTimeService.systemUtc();

    assertEquals(new UtcTime(1L, 2L, (short) -60), service.newUniversalTime(1L, 2L, (short) -60));

    TimeServiceException negative =
        assertThrows(
            TimeServiceException.class, () -> service.newUniversalTime(1L, -1L, (short) 0));
    TimeServiceException overflow =
        assertThrows(
            TimeServiceException.class,
            () -> service.newUniversalTime(1L, UtcTime.MAX_INACCURACY_TICKS + 1L, (short) 0));

    assertEquals(TimeServiceDiagnosticCodes.INVALID_INACCURACY, negative.code());
    assertEquals(TimeServiceDiagnosticCodes.INVALID_INACCURACY, overflow.code());
  }

  @Test
  void createsIntervalsAndRejectsInvalidBounds() {
    LocalTimeService service = LocalTimeService.systemUtc();

    assertEquals(new TimeInterval(3L, 5L), service.newInterval(3L, 5L));

    TimeServiceException negative =
        assertThrows(TimeServiceException.class, () -> service.newInterval(-1L, 5L));
    TimeServiceException unordered =
        assertThrows(TimeServiceException.class, () -> service.newInterval(6L, 5L));

    assertEquals(TimeServiceDiagnosticCodes.INVALID_INTERVAL, negative.code());
    assertEquals(TimeServiceDiagnosticCodes.INVALID_INTERVAL, unordered.code());
  }

  @Test
  void optionsRejectInvalidInputsAndOffsetPrecision() {
    assertThrows(
        NullPointerException.class,
        () -> new TimeServiceOptions(null, Duration.ZERO, ZoneOffset.UTC));
    assertThrows(
        NullPointerException.class,
        () -> new TimeServiceOptions(Clock.systemUTC(), null, ZoneOffset.UTC));
    assertThrows(
        NullPointerException.class,
        () -> new TimeServiceOptions(Clock.systemUTC(), Duration.ZERO, null));

    TimeServiceException negative =
        assertThrows(
            TimeServiceException.class,
            () -> new TimeServiceOptions(Clock.systemUTC(), Duration.ofNanos(-1), ZoneOffset.UTC));
    TimeServiceException tooLarge =
        assertThrows(
            TimeServiceException.class,
            () ->
                new TimeServiceOptions(
                    Clock.systemUTC(),
                    Duration.ofSeconds((UtcTime.MAX_INACCURACY_TICKS / 10_000_000L) + 1L),
                    ZoneOffset.UTC));
    TimeServiceException subMinuteOffset =
        assertThrows(
            TimeServiceException.class,
            () ->
                new TimeServiceOptions(
                    Clock.systemUTC(), Duration.ZERO, ZoneOffset.ofTotalSeconds(1)));

    assertEquals(TimeServiceDiagnosticCodes.INVALID_INACCURACY, negative.code());
    assertEquals(TimeServiceDiagnosticCodes.INVALID_INACCURACY, tooLarge.code());
    assertEquals(TimeServiceDiagnosticCodes.INVALID_TDF, subMinuteOffset.code());
  }
}
