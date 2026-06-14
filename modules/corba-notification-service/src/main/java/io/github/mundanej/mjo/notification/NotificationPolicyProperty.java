package io.github.mundanej.mjo.notification;

/** Caller-supplied Notification Service QoS/admin policy property. */
public record NotificationPolicyProperty(
    NotificationPolicyKey key, NotificationPrimitiveValue value) {

  /** Creates a validated policy property. */
  public NotificationPolicyProperty {
    key = NotificationPolicyKey.requirePolicyPresent("policy key", key);
    value = NotificationPolicyKey.requirePolicyPresent("policy value", value);
  }

  /** Creates a signed integer policy property by external key. */
  public static NotificationPolicyProperty signedLong(String key, long value) {
    return new NotificationPolicyProperty(
        NotificationPolicyKey.fromKey(key), NotificationPrimitiveValue.signedLongValue(value));
  }

  /** Creates a boolean policy property by external key. */
  public static NotificationPolicyProperty bool(String key, boolean value) {
    return new NotificationPolicyProperty(
        NotificationPolicyKey.fromKey(key), NotificationPrimitiveValue.booleanValue(value));
  }
}
