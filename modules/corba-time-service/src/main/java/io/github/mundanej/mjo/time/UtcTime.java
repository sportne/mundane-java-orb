package io.github.mundanej.mjo.time;

/** Immutable TimeBase UtcT-compatible value for the supported local Time Service subset. */
public record UtcTime(long timeTicks, long inaccuracyTicks, short tdfMinutes) {

  /** Number of inaccuracy bits exposed by TimeBase UtcT inacchi/inacclo. */
  public static final int INACCURACY_BITS = 48;

  /** Maximum inaccuracy tick value representable by TimeBase UtcT. */
  public static final long MAX_INACCURACY_TICKS = (1L << INACCURACY_BITS) - 1L;

  /** Creates a validated UTC time value. */
  public UtcTime {
    if (timeTicks < 0) {
      throw new TimeServiceException(
          TimeServiceDiagnosticCodes.INVALID_TIME, "timeTicks must be nonnegative");
    }
    if (inaccuracyTicks < 0 || inaccuracyTicks > MAX_INACCURACY_TICKS) {
      throw new TimeServiceException(
          TimeServiceDiagnosticCodes.INVALID_INACCURACY,
          "inaccuracyTicks must fit in TimeBase 48-bit inaccuracy");
    }
  }
}
