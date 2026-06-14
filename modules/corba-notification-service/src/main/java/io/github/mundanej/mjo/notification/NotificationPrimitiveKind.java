package io.github.mundanej.mjo.notification;

/** Supported primitive value kinds for the local structured-event subset. */
public enum NotificationPrimitiveKind {
  /** UTF-16 Java text value bounded by the Notification Service model. */
  STRING,

  /** Boolean filter or body value. */
  BOOLEAN,

  /** Signed 64-bit integer value. */
  SIGNED_LONG,

  /** Finite 64-bit floating-point value. */
  FLOATING_POINT
}
