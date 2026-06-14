package io.github.mundanej.mjo.trading;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for supported Trading Service failures. */
public final class TradingServiceDiagnosticCodes {

  /** A configured Trading Service limit was outside the supported range. */
  public static final DiagnosticCode INVALID_LIMIT = new DiagnosticCode("TRAD-0001");

  /** A type or property name was blank, missing, or oversized. */
  public static final DiagnosticCode MALFORMED_NAME = new DiagnosticCode("TRAD-0002");

  /** A service type registration used a name that already exists. */
  public static final DiagnosticCode TYPE_ALREADY_EXISTS = new DiagnosticCode("TRAD-0003");

  /** A service type lookup, update, or deletion referenced an unknown type. */
  public static final DiagnosticCode TYPE_NOT_FOUND = new DiagnosticCode("TRAD-0004");

  /** The configured service type count has been reached. */
  public static final DiagnosticCode TYPE_LIMIT_EXCEEDED = new DiagnosticCode("TRAD-0005");

  /** A service type exceeded the configured property definition count. */
  public static final DiagnosticCode PROPERTY_LIMIT_EXCEEDED = new DiagnosticCode("TRAD-0006");

  /** A service type declared the same property name more than once. */
  public static final DiagnosticCode DUPLICATE_PROPERTY = new DiagnosticCode("TRAD-0007");

  /** A service type or property definition was null or internally inconsistent. */
  public static final DiagnosticCode MALFORMED_TYPE = new DiagnosticCode("TRAD-0008");

  private TradingServiceDiagnosticCodes() {}
}
