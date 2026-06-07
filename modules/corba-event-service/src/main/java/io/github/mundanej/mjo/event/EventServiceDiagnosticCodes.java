package io.github.mundanej.mjo.event;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for supported Event Service failures. */
public final class EventServiceDiagnosticCodes {

  /** A configured Event Service limit was outside the supported range. */
  public static final DiagnosticCode INVALID_LIMIT = new DiagnosticCode("EVNT-0001");

  /** The local Event Service was already shut down. */
  public static final DiagnosticCode SERVICE_SHUTDOWN = new DiagnosticCode("EVNT-0002");

  /** The event channel was already destroyed or no longer belongs to the service. */
  public static final DiagnosticCode CHANNEL_DESTROYED = new DiagnosticCode("EVNT-0003");

  /** The proxy handle was already destroyed. */
  public static final DiagnosticCode PROXY_DESTROYED = new DiagnosticCode("EVNT-0004");

  /** The configured channel count has been reached. */
  public static final DiagnosticCode CHANNEL_LIMIT_EXCEEDED = new DiagnosticCode("EVNT-0005");

  /** A null or unsupported event payload was supplied. */
  public static final DiagnosticCode INVALID_PAYLOAD = new DiagnosticCode("EVNT-0006");

  /** A proxy operation required a local callback connection that is absent. */
  public static final DiagnosticCode PROXY_NOT_CONNECTED = new DiagnosticCode("EVNT-0007");

  /** A proxy connection was attempted while a callback was already connected. */
  public static final DiagnosticCode CONNECTION_ALREADY_ACTIVE = new DiagnosticCode("EVNT-0008");

  /** A pull operation found no local event payload available. */
  public static final DiagnosticCode NO_EVENT_AVAILABLE = new DiagnosticCode("EVNT-0009");

  private EventServiceDiagnosticCodes() {}
}
