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

  private EventServiceDiagnosticCodes() {}
}
