package io.github.mundanej.mjo.notification;

import java.util.Locale;

/** Supported local Notification Service QoS/admin policy keys. */
public enum NotificationPolicyKey {
  /** Maximum queued structured events per proxy. */
  QUEUE_LIMIT("queue-limit"),

  /** Maximum channels owned by one local service. */
  CHANNEL_LIMIT("channel-limit"),

  /** Maximum supplier-side admin proxy handles owned by one channel. */
  SUPPLIER_ADMIN_LIMIT("supplier-admin-limit"),

  /** Maximum consumer-side admin proxy handles owned by one channel. */
  CONSUMER_ADMIN_LIMIT("consumer-admin-limit"),

  /** Maximum proxy handles owned by one channel across both admin sides. */
  PROXY_LIMIT("proxy-limit"),

  /** Maximum filter expression length accepted by the local subset. */
  FILTER_LENGTH_LIMIT("filter-length-limit"),

  /** Maximum nested filter expression depth accepted by the local subset. */
  FILTER_DEPTH_LIMIT("filter-depth-limit"),

  /** Maximum filter boolean/comparison terms accepted by the local subset. */
  FILTER_TERM_LIMIT("filter-term-limit"),

  /** Whether durable delivery is requested. The local subset supports false only. */
  DURABLE("durable"),

  /** Whether transaction integration is requested. The local subset supports false only. */
  TRANSACTED("transacted");

  private final String key;

  NotificationPolicyKey(String key) {
    this.key = key;
  }

  /** Returns the stable external policy key. */
  public String key() {
    return key;
  }

  /** Resolves a supported policy key. */
  public static NotificationPolicyKey fromKey(String key) {
    String checked = requirePolicyPresent("policy key", key);
    for (NotificationPolicyKey value : values()) {
      if (value.key.equals(checked.toLowerCase(Locale.ROOT))) {
        return value;
      }
    }
    throw new NotificationServiceException(
        NotificationServiceDiagnosticCodes.UNSUPPORTED_POLICY,
        "unsupported Notification Service policy key: " + key);
  }

  static <T> T requirePolicyPresent(String name, T value) {
    if (value == null) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.MALFORMED_POLICY, name + " must not be null");
    }
    return value;
  }
}
