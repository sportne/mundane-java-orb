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

  /** An offer registration used an ID that already exists. */
  public static final DiagnosticCode OFFER_ALREADY_EXISTS = new DiagnosticCode("TRAD-0009");

  /** An offer lookup, update, or withdrawal referenced an unknown offer. */
  public static final DiagnosticCode OFFER_NOT_FOUND = new DiagnosticCode("TRAD-0010");

  /** The configured offer count has been reached. */
  public static final DiagnosticCode OFFER_LIMIT_EXCEEDED = new DiagnosticCode("TRAD-0011");

  /** An offer property did not match its service type definition. */
  public static final DiagnosticCode PROPERTY_MISMATCH = new DiagnosticCode("TRAD-0012");

  /** An offer property value used an unsupported Java value type or state. */
  public static final DiagnosticCode UNSUPPORTED_VALUE = new DiagnosticCode("TRAD-0013");

  /** An offer was null or internally inconsistent. */
  public static final DiagnosticCode MALFORMED_OFFER = new DiagnosticCode("TRAD-0014");

  /** An offer property value exceeded a configured bound. */
  public static final DiagnosticCode VALUE_LIMIT_EXCEEDED = new DiagnosticCode("TRAD-0015");

  /** A Trading Service constraint expression was malformed. */
  public static final DiagnosticCode MALFORMED_CONSTRAINT = new DiagnosticCode("TRAD-0016");

  /** A Trading Service constraint expression exceeded a configured parser bound. */
  public static final DiagnosticCode CONSTRAINT_LIMIT_EXCEEDED = new DiagnosticCode("TRAD-0017");

  /** A Trading Service constraint expression used unsupported syntax or operators. */
  public static final DiagnosticCode UNSUPPORTED_CONSTRAINT_OPERATOR =
      new DiagnosticCode("TRAD-0018");

  /** Constraint evaluation referenced a property that was not present. */
  public static final DiagnosticCode UNKNOWN_CONSTRAINT_PROPERTY = new DiagnosticCode("TRAD-0019");

  /** Constraint evaluation compared incompatible primitive value kinds. */
  public static final DiagnosticCode CONSTRAINT_TYPE_MISMATCH = new DiagnosticCode("TRAD-0020");

  /** A local Trading Service query exceeded configured result or cost limits. */
  public static final DiagnosticCode QUERY_LIMIT_EXCEEDED = new DiagnosticCode("TRAD-0021");

  /** An import/export link registration used a name that already exists. */
  public static final DiagnosticCode LINK_ALREADY_EXISTS = new DiagnosticCode("TRAD-0022");

  /** An import/export link lookup, update, or removal referenced an unknown link. */
  public static final DiagnosticCode LINK_NOT_FOUND = new DiagnosticCode("TRAD-0023");

  /** The configured import/export link count has been reached. */
  public static final DiagnosticCode LINK_LIMIT_EXCEEDED = new DiagnosticCode("TRAD-0024");

  /** An import/export link was null or internally inconsistent. */
  public static final DiagnosticCode MALFORMED_LINK = new DiagnosticCode("TRAD-0025");

  /** Remote Trading Service federation is not implemented in the supported local subset. */
  public static final DiagnosticCode REMOTE_FEDERATION_DISABLED = new DiagnosticCode("TRAD-0026");

  /** An import/export operation referenced a link with the wrong direction. */
  public static final DiagnosticCode LINK_DIRECTION_MISMATCH = new DiagnosticCode("TRAD-0027");

  private TradingServiceDiagnosticCodes() {}
}
