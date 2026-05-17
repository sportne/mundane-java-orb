package io.github.mundanej.mjo.iiop;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for IIOP TCP failures. */
public final class IiopDiagnosticCodes {

  /** The client or server was used after close or before it was ready. */
  public static final DiagnosticCode LIFECYCLE = new DiagnosticCode("IIOP-0001");

  /** A TCP connection attempt timed out. */
  public static final DiagnosticCode CONNECT_TIMEOUT = new DiagnosticCode("IIOP-0002");

  /** A socket read timed out. */
  public static final DiagnosticCode READ_TIMEOUT = new DiagnosticCode("IIOP-0003");

  /** The peer closed the connection before a full frame was available. */
  public static final DiagnosticCode EOF = new DiagnosticCode("IIOP-0004");

  /** A frame exceeded configured message or body bounds. */
  public static final DiagnosticCode FRAME_LIMIT = new DiagnosticCode("IIOP-0005");

  /** A message kind was not supported at this point in the TCP request path. */
  public static final DiagnosticCode UNSUPPORTED_MESSAGE = new DiagnosticCode("IIOP-0006");

  /** A reply did not match the outstanding request id. */
  public static final DiagnosticCode CORRELATION_FAILURE = new DiagnosticCode("IIOP-0007");

  /** Request handling failed before a reply could be written. */
  public static final DiagnosticCode HANDLER_FAILURE = new DiagnosticCode("IIOP-0008");

  /** A general socket connection failure occurred. */
  public static final DiagnosticCode CONNECTION_FAILURE = new DiagnosticCode("IIOP-0009");

  /** Endpoint or option values were invalid. */
  public static final DiagnosticCode INVALID_CONFIGURATION = new DiagnosticCode("IIOP-0010");

  /** TLS negotiation failed before GIOP exchange could continue. */
  public static final DiagnosticCode TLS_HANDSHAKE_FAILURE = new DiagnosticCode("IIOP-0011");

  private IiopDiagnosticCodes() {}
}
