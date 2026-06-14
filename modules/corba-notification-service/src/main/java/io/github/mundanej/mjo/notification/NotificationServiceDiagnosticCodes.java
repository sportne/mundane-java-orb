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

  /** A structured-event identity, property name, or section was malformed. */
  public static final DiagnosticCode MALFORMED_STRUCTURED_EVENT = new DiagnosticCode("NOTF-0008");

  /** A structured-event section repeated a property name. */
  public static final DiagnosticCode DUPLICATE_FIELD = new DiagnosticCode("NOTF-0009");

  /** A structured-event section exceeded the supported field count. */
  public static final DiagnosticCode FIELD_LIMIT_EXCEEDED = new DiagnosticCode("NOTF-0010");

  /** A structured-event primitive value used an unsupported Java representation. */
  public static final DiagnosticCode UNSUPPORTED_VALUE = new DiagnosticCode("NOTF-0011");

  /** A structured-event string value exceeded the supported length. */
  public static final DiagnosticCode VALUE_LIMIT_EXCEEDED = new DiagnosticCode("NOTF-0012");

  /** A Notification Service filter expression was malformed. */
  public static final DiagnosticCode MALFORMED_FILTER = new DiagnosticCode("NOTF-0013");

  /** A Notification Service filter expression exceeded supported limits. */
  public static final DiagnosticCode FILTER_LIMIT_EXCEEDED = new DiagnosticCode("NOTF-0014");

  /** A Notification Service filter expression used an unsupported operator. */
  public static final DiagnosticCode UNSUPPORTED_FILTER_OPERATOR = new DiagnosticCode("NOTF-0015");

  /** A Notification Service filter referenced an unknown event field. */
  public static final DiagnosticCode UNKNOWN_FILTER_FIELD = new DiagnosticCode("NOTF-0016");

  /** A Notification Service filter compared incompatible primitive types. */
  public static final DiagnosticCode FILTER_TYPE_MISMATCH = new DiagnosticCode("NOTF-0017");

  private NotificationServiceDiagnosticCodes() {}
}
