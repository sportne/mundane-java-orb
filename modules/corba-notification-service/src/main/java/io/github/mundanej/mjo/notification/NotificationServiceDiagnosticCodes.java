package io.github.mundanej.mjo.notification;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes for supported Notification Service failures. */
public final class NotificationServiceDiagnosticCodes {

  /** A configured Notification Service limit was outside the supported range. */
  public static final DiagnosticCode INVALID_LIMIT = new DiagnosticCode("NOTF-0001");

  /** The local Notification Service was already shut down. */
  public static final DiagnosticCode SERVICE_SHUTDOWN = new DiagnosticCode("NOTF-0002");

  /** The notification channel was already destroyed or no longer belongs to the service. */
  public static final DiagnosticCode CHANNEL_DESTROYED = new DiagnosticCode("NOTF-0003");

  /** The proxy handle was already destroyed. */
  public static final DiagnosticCode PROXY_DESTROYED = new DiagnosticCode("NOTF-0004");

  /** The configured channel count has been reached. */
  public static final DiagnosticCode CHANNEL_LIMIT_EXCEEDED = new DiagnosticCode("NOTF-0005");

  /** The configured supplier-side proxy count has been reached. */
  public static final DiagnosticCode SUPPLIER_LIMIT_EXCEEDED = new DiagnosticCode("NOTF-0006");

  /** The configured consumer-side proxy count has been reached. */
  public static final DiagnosticCode CONSUMER_LIMIT_EXCEEDED = new DiagnosticCode("NOTF-0007");

  private NotificationServiceDiagnosticCodes() {}
}
