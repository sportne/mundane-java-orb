package io.github.mundanej.mjo.time;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for supported Time Service failures. */
public final class TimeServiceDiagnosticCodes {

  /** Universal time ticks were before the supported Gregorian epoch or overflowed. */
  public static final DiagnosticCode INVALID_TIME = new DiagnosticCode("TIME-0001");

  /** Inaccuracy was negative, overflowed, or exceeded the TimeBase 48-bit field. */
  public static final DiagnosticCode INVALID_INACCURACY = new DiagnosticCode("TIME-0002");

  /** Time displacement factor could not be represented as supported minutes. */
  public static final DiagnosticCode INVALID_TDF = new DiagnosticCode("TIME-0003");

  /** Interval bounds were negative or not ordered. */
  public static final DiagnosticCode INVALID_INTERVAL = new DiagnosticCode("TIME-0004");

  /** The configured clock failed while reading universal time. */
  public static final DiagnosticCode CLOCK_UNAVAILABLE = new DiagnosticCode("TIME-0005");

  private TimeServiceDiagnosticCodes() {}
}
