package io.github.mundanej.mjo.notification;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Validated QoS/admin policy limits for the supported local Notification Service subset. */
public record NotificationPolicies(
    int queueLimit,
    int channelLimit,
    int supplierAdminLimit,
    int consumerAdminLimit,
    int proxyLimit,
    int filterLengthLimit,
    int filterDepthLimit,
    int filterTermLimit,
    boolean durable,
    boolean transacted) {

  /** Default per-proxy queue limit reserved for the local delivery slice. */
  public static final int DEFAULT_QUEUE_LIMIT = 64;

  /** Maximum supported queue limit. */
  public static final int MAX_QUEUE_LIMIT = 65_535;

  /** Maximum supported channel, admin, and proxy resource limits. */
  public static final int MAX_RESOURCE_LIMIT = 65_535;

  /** Creates validated policies. */
  public NotificationPolicies {
    requireRange(queueLimit, "queue-limit", 1, MAX_QUEUE_LIMIT);
    requireRange(channelLimit, "channel-limit", 1, MAX_RESOURCE_LIMIT);
    requireRange(supplierAdminLimit, "supplier-admin-limit", 1, MAX_RESOURCE_LIMIT);
    requireRange(consumerAdminLimit, "consumer-admin-limit", 1, MAX_RESOURCE_LIMIT);
    requireRange(proxyLimit, "proxy-limit", 1, MAX_RESOURCE_LIMIT);
    requireRange(
        filterLengthLimit, "filter-length-limit", 1, NotificationFilter.MAX_EXPRESSION_LENGTH);
    requireRange(filterDepthLimit, "filter-depth-limit", 1, NotificationFilter.MAX_DEPTH);
    requireRange(filterTermLimit, "filter-term-limit", 1, NotificationFilter.MAX_TERMS);
    if (proxyLimit < supplierAdminLimit || proxyLimit < consumerAdminLimit) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.CONFLICTING_POLICY,
          "proxy-limit must be at least each admin-side proxy limit");
    }
    if (durable) {
      throw unsupportedTrue(NotificationPolicyKey.DURABLE);
    }
    if (transacted) {
      throw unsupportedTrue(NotificationPolicyKey.TRANSACTED);
    }
  }

  /** Returns local default policies. */
  public static NotificationPolicies defaults() {
    return new NotificationPolicies(
        DEFAULT_QUEUE_LIMIT,
        NotificationServiceOptions.DEFAULT_MAX_CHANNELS,
        NotificationServiceOptions.DEFAULT_MAX_SUPPLIERS_PER_CHANNEL,
        NotificationServiceOptions.DEFAULT_MAX_CONSUMERS_PER_CHANNEL,
        NotificationServiceOptions.DEFAULT_MAX_SUPPLIERS_PER_CHANNEL
            + NotificationServiceOptions.DEFAULT_MAX_CONSUMERS_PER_CHANNEL,
        NotificationFilter.MAX_EXPRESSION_LENGTH,
        NotificationFilter.MAX_DEPTH,
        NotificationFilter.MAX_TERMS,
        false,
        false);
  }

  /** Builds validated policies from caller-supplied properties. */
  public static NotificationPolicies from(List<NotificationPolicyProperty> properties) {
    List<NotificationPolicyProperty> checked =
        NotificationPolicyKey.requirePolicyPresent("policy properties", properties);
    Map<NotificationPolicyKey, NotificationPrimitiveValue> values =
        new EnumMap<>(NotificationPolicyKey.class);
    for (NotificationPolicyProperty property : checked) {
      NotificationPolicyProperty present =
          NotificationPolicyKey.requirePolicyPresent("policy property", property);
      if (values.putIfAbsent(present.key(), present.value()) != null) {
        throw new NotificationServiceException(
            NotificationServiceDiagnosticCodes.DUPLICATE_POLICY,
            "duplicate Notification Service policy key: " + present.key().key());
      }
    }

    NotificationPolicies defaults = defaults();
    int queueLimit = intPolicy(values, NotificationPolicyKey.QUEUE_LIMIT, defaults.queueLimit);
    int channelLimit =
        intPolicy(values, NotificationPolicyKey.CHANNEL_LIMIT, defaults.channelLimit);
    int supplierAdminLimit =
        intPolicy(values, NotificationPolicyKey.SUPPLIER_ADMIN_LIMIT, defaults.supplierAdminLimit);
    int consumerAdminLimit =
        intPolicy(values, NotificationPolicyKey.CONSUMER_ADMIN_LIMIT, defaults.consumerAdminLimit);
    int proxyLimit = intPolicy(values, NotificationPolicyKey.PROXY_LIMIT, defaults.proxyLimit);
    int filterLengthLimit =
        intPolicy(values, NotificationPolicyKey.FILTER_LENGTH_LIMIT, defaults.filterLengthLimit);
    int filterDepthLimit =
        intPolicy(values, NotificationPolicyKey.FILTER_DEPTH_LIMIT, defaults.filterDepthLimit);
    int filterTermLimit =
        intPolicy(values, NotificationPolicyKey.FILTER_TERM_LIMIT, defaults.filterTermLimit);
    boolean durable = boolPolicy(values, NotificationPolicyKey.DURABLE, defaults.durable);
    boolean transacted = boolPolicy(values, NotificationPolicyKey.TRANSACTED, defaults.transacted);
    return new NotificationPolicies(
        queueLimit,
        channelLimit,
        supplierAdminLimit,
        consumerAdminLimit,
        proxyLimit,
        filterLengthLimit,
        filterDepthLimit,
        filterTermLimit,
        durable,
        transacted);
  }

  private static int intPolicy(
      Map<NotificationPolicyKey, NotificationPrimitiveValue> values,
      NotificationPolicyKey key,
      int defaultValue) {
    NotificationPrimitiveValue value = values.get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value.kind() != NotificationPrimitiveKind.SIGNED_LONG) {
      throw malformed(key, "must be a signed integer");
    }
    long signedLong = value.asSignedLong();
    if (signedLong < Integer.MIN_VALUE || signedLong > Integer.MAX_VALUE) {
      throw limit(key, "is outside supported integer bounds");
    }
    return (int) signedLong;
  }

  private static boolean boolPolicy(
      Map<NotificationPolicyKey, NotificationPrimitiveValue> values,
      NotificationPolicyKey key,
      boolean defaultValue) {
    NotificationPrimitiveValue value = values.get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value.kind() != NotificationPrimitiveKind.BOOLEAN) {
      throw malformed(key, "must be a boolean");
    }
    return value.asBoolean();
  }

  private static void requireRange(int value, String name, int minimum, int maximum) {
    if (value < minimum || value > maximum) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.POLICY_LIMIT_EXCEEDED,
          name + " must be between " + minimum + " and " + maximum);
    }
  }

  private static NotificationServiceException malformed(NotificationPolicyKey key, String message) {
    return new NotificationServiceException(
        NotificationServiceDiagnosticCodes.MALFORMED_POLICY, key.key() + " " + message);
  }

  private static NotificationServiceException limit(NotificationPolicyKey key, String message) {
    return new NotificationServiceException(
        NotificationServiceDiagnosticCodes.POLICY_LIMIT_EXCEEDED, key.key() + " " + message);
  }

  private static NotificationServiceException unsupportedTrue(NotificationPolicyKey key) {
    return new NotificationServiceException(
        NotificationServiceDiagnosticCodes.UNSUPPORTED_POLICY,
        key.key() + " true is not supported by the local subset");
  }
}
