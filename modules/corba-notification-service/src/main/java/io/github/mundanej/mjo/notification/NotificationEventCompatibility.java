package io.github.mundanej.mjo.notification;

/** Stable local identity for the Event Service compatibility boundary. */
public record NotificationEventCompatibility(
    String notificationService, String eventService, boolean deliveryCompatible) {

  /** Creates the G8-310 local compatibility boundary identity. */
  public static NotificationEventCompatibility localBoundary() {
    return new NotificationEventCompatibility(
        "CosNotification::EventChannel", "CosEventChannelAdmin::EventChannel", false);
  }

  /** Creates a validated compatibility record. */
  public NotificationEventCompatibility {
    notificationService = requireNonBlank(notificationService, "notificationService");
    eventService = requireNonBlank(eventService, "eventService");
  }

  private static String requireNonBlank(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
