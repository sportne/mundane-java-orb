package io.github.mundanej.mjo.time;

/** Immutable TimeBase IntervalT-compatible value for the supported local Time Service subset. */
public record TimeInterval(long lowerBoundTicks, long upperBoundTicks) {

  /** Creates a validated time interval value. */
  public TimeInterval {
    if (lowerBoundTicks < 0 || upperBoundTicks < 0) {
      throw new TimeServiceException(
          TimeServiceDiagnosticCodes.INVALID_INTERVAL, "interval bounds must be nonnegative");
    }
    if (lowerBoundTicks > upperBoundTicks) {
      throw new TimeServiceException(
          TimeServiceDiagnosticCodes.INVALID_INTERVAL,
          "lowerBoundTicks must not exceed upperBoundTicks");
    }
  }
}
