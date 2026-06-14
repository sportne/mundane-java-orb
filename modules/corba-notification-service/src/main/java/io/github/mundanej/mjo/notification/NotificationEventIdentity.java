package io.github.mundanej.mjo.notification;

/** Fixed Notification Service structured-event identity. */
public record NotificationEventIdentity(NotificationEventType eventType, String eventName) {

  /** Creates a validated structured-event identity. */
  public NotificationEventIdentity {
    eventType = NotificationStructuredEvent.requirePresent("eventType", eventType);
    eventName = NotificationStructuredEvent.requireName(eventName, "event name");
  }
}
