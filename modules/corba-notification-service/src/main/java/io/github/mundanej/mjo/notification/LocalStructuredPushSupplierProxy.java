package io.github.mundanej.mjo.notification;

import java.util.concurrent.atomic.AtomicReference;

/** Local lifecycle handle for a structured push-supplier proxy. */
public final class LocalStructuredPushSupplierProxy extends LocalNotificationProxy {

  private static final NotificationFilter MATCH_ALL = NotificationFilter.parse("true");

  private final LocalNotificationChannel channel;
  private final AtomicReference<NotificationPushConsumer> consumer = new AtomicReference<>();
  private volatile NotificationFilter filter = MATCH_ALL;
  private volatile NotificationPolicies policies = NotificationPolicies.defaults();
  private volatile boolean failed;

  LocalStructuredPushSupplierProxy(LocalNotificationChannel channel, long id) {
    super(channel, id, NotificationProxyKind.STRUCTURED_PUSH_SUPPLIER);
    this.channel = channel;
  }

  /** Connects the local structured push consumer callback to this proxy. */
  public void connectStructuredPushConsumer(NotificationPushConsumer consumer) {
    connectStructuredPushConsumer(consumer, MATCH_ALL, NotificationPolicies.defaults());
  }

  /** Connects the local structured push consumer callback with a bounded filter. */
  public void connectStructuredPushConsumer(
      NotificationPushConsumer consumer, NotificationFilter filter) {
    connectStructuredPushConsumer(consumer, filter, NotificationPolicies.defaults());
  }

  /** Connects the local structured push consumer callback with bounded filter and policies. */
  public void connectStructuredPushConsumer(
      NotificationPushConsumer consumer, NotificationFilter filter, NotificationPolicies policies) {
    requireAlive();
    channel.requireActive(this);
    requireUsable();
    if (consumer == null) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.PROXY_NOT_CONNECTED,
          "structured push consumer must not be null");
    }
    NotificationFilter checkedFilter = requireFilter(filter);
    NotificationPolicies checkedPolicies = requirePolicies(policies);
    requireFilterWithinPolicy(checkedFilter, checkedPolicies);
    if (!this.consumer.compareAndSet(null, consumer)) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.CONNECTION_ALREADY_ACTIVE,
          "structured push consumer is already connected");
    }
    this.filter = checkedFilter;
    this.policies = checkedPolicies;
  }

  /** Disconnects the local structured push consumer callback if present. */
  public void disconnectStructuredPushConsumer() {
    NotificationPushConsumer connected = consumer.getAndSet(null);
    if (connected != null) {
      connected.disconnectStructuredPushConsumer();
    }
  }

  /** Returns whether a local structured push consumer is connected. */
  public boolean isConnected() {
    return consumer.get() != null;
  }

  void deliver(NotificationStructuredEvent event) {
    requireAlive();
    channel.requireActive(this);
    requireUsable();
    NotificationPushConsumer connected = consumer.get();
    if (connected != null && filter.evaluate(LocalNotificationChannel.requireEvent(event))) {
      connected.pushStructuredEvent(event);
    }
  }

  void configureFilter(NotificationFilter filter) {
    requireAlive();
    channel.requireActive(this);
    NotificationFilter checkedFilter = requireFilter(filter);
    requireFilterWithinPolicy(checkedFilter, policies);
    this.filter = checkedFilter;
  }

  void configurePolicies(NotificationPolicies policies) {
    requireAlive();
    channel.requireActive(this);
    NotificationPolicies checkedPolicies = requirePolicies(policies);
    requireFilterWithinPolicy(filter, checkedPolicies);
    this.policies = checkedPolicies;
  }

  void markFailed() {
    failed = true;
  }

  int proxyLimit() {
    return policies.proxyLimit();
  }

  private void requireUsable() {
    if (failed) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.CONSUMER_DELIVERY_FAILED,
          "structured push consumer proxy failed previously: " + id());
    }
  }

  private static NotificationFilter requireFilter(NotificationFilter filter) {
    if (filter == null) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.MALFORMED_FILTER, "filter must not be null");
    }
    return filter;
  }

  private static NotificationPolicies requirePolicies(NotificationPolicies policies) {
    if (policies == null) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.MALFORMED_POLICY, "policies must not be null");
    }
    return policies;
  }

  private static void requireFilterWithinPolicy(
      NotificationFilter filter, NotificationPolicies policies) {
    if (filter.expression().length() > policies.filterLengthLimit()) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.FILTER_LIMIT_EXCEEDED,
          "filter expression exceeds configured length limit");
    }
    if (filter.maxDepth() > policies.filterDepthLimit()) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.FILTER_LIMIT_EXCEEDED,
          "filter expression exceeds configured depth limit");
    }
    if (filter.termCount() > policies.filterTermLimit()) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.FILTER_LIMIT_EXCEEDED,
          "filter expression exceeds configured term limit");
    }
  }

  @Override
  void onDestroy() {
    disconnectStructuredPushConsumer();
  }
}
