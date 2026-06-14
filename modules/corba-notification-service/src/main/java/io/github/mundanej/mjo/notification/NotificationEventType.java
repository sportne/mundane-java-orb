package io.github.mundanej.mjo.notification;

/** Fixed Notification Service event type identity. */
public record NotificationEventType(String domainName, String typeName) {

  /** Creates a validated event type. */
  public NotificationEventType {
    domainName = NotificationStructuredEvent.requireName(domainName, "domain name");
    typeName = NotificationStructuredEvent.requireName(typeName, "type name");
  }
}
