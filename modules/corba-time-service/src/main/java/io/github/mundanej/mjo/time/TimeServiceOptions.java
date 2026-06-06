package io.github.mundanej.mjo.time;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Objects;

/** Caller-provided local Time Service clock policy. */
public record TimeServiceOptions(Clock clock, Duration inaccuracy, ZoneOffset offset) {

  /** Creates validated Time Service options. */
  public TimeServiceOptions {
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(inaccuracy, "inaccuracy");
    Objects.requireNonNull(offset, "offset");
    if (inaccuracy.isNegative()) {
      throw new TimeServiceException(
          TimeServiceDiagnosticCodes.INVALID_INACCURACY, "inaccuracy must be nonnegative");
    }
    LocalTimeService.inaccuracyTicks(inaccuracy);
    if (offset.getTotalSeconds() % 60 != 0) {
      throw new TimeServiceException(
          TimeServiceDiagnosticCodes.INVALID_TDF, "offset must be representable in minutes");
    }
  }

  /** Returns options using the system UTC clock and no declared inaccuracy. */
  public static TimeServiceOptions systemUtc() {
    return new TimeServiceOptions(Clock.systemUTC(), Duration.ZERO, ZoneOffset.UTC);
  }
}
