package io.github.mundanej.mjo.notification;

/** Named primitive property used by local structured-event sections. */
public record NotificationProperty(String name, NotificationPrimitiveValue value) {

  /** Creates a validated structured-event property. */
  public NotificationProperty {
    name = NotificationStructuredEvent.requireName(name, "property name");
    value = NotificationStructuredEvent.requirePresent("property value", value);
  }

  /** Creates a string property. */
  public static NotificationProperty stringProperty(String name, String value) {
    return new NotificationProperty(name, NotificationPrimitiveValue.stringValue(value));
  }

  /** Creates a boolean property. */
  public static NotificationProperty booleanProperty(String name, boolean value) {
    return new NotificationProperty(name, NotificationPrimitiveValue.booleanValue(value));
  }

  /** Creates a signed 64-bit integer property. */
  public static NotificationProperty signedLongProperty(String name, long value) {
    return new NotificationProperty(name, NotificationPrimitiveValue.signedLongValue(value));
  }

  /** Creates a finite floating-point property. */
  public static NotificationProperty floatingPointProperty(String name, double value) {
    return new NotificationProperty(name, NotificationPrimitiveValue.floatingPointValue(value));
  }
}
