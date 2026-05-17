package io.github.mundanej.mjo.ior;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for IOR and object URL failures. */
public final class IorDiagnosticCodes {

  /** Encoded input ended before the IOR value was complete. */
  public static final DiagnosticCode TRUNCATED_INPUT = new DiagnosticCode("IOR-0001");

  /** Encoded or decoded length exceeded the configured IOR bounds. */
  public static final DiagnosticCode LENGTH_LIMIT_EXCEEDED = new DiagnosticCode("IOR-0002");

  /** A profile, component, or URL tag was outside the unsigned-long range. */
  public static final DiagnosticCode TAG_OUT_OF_RANGE = new DiagnosticCode("IOR-0003");

  /** A port value was outside the unsigned-short range. */
  public static final DiagnosticCode INVALID_PORT = new DiagnosticCode("IOR-0004");

  /** A stringified IOR did not match the required IOR:hex form. */
  public static final DiagnosticCode INVALID_STRINGIFIED_IOR = new DiagnosticCode("IOR-0005");

  /** A corbaloc or corbaname value did not match the supported URL grammar. */
  public static final DiagnosticCode INVALID_OBJECT_URL = new DiagnosticCode("IOR-0006");

  /** An IIOP profile body violated the supported IIOP profile structure. */
  public static final DiagnosticCode INVALID_IIOP_PROFILE = new DiagnosticCode("IOR-0007");

  private IorDiagnosticCodes() {}
}
