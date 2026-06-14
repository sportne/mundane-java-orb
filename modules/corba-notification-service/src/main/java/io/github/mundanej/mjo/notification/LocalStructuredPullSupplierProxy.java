package io.github.mundanej.mjo.notification;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Local lifecycle handle for a structured pull-supplier proxy. */
public final class LocalStructuredPullSupplierProxy extends LocalNotificationProxy {

  private static final NotificationFilter MATCH_ALL = NotificationFilter.parse("true");

  private final LocalNotificationChannel channel;
  private final AtomicReference<NotificationPullConsumer> consumer = new AtomicReference<>();
  private final Deque<NotificationStructuredEvent> queue = new ArrayDeque<>();
  private volatile NotificationFilter filter = MATCH_ALL;
  private volatile NotificationPolicies policies = NotificationPolicies.defaults();

  LocalStructuredPullSupplierProxy(LocalNotificationChannel channel, long id) {
    super(channel, id, NotificationProxyKind.STRUCTURED_PULL_SUPPLIER);
    this.channel = channel;
  }

  /** Connects the local structured pull consumer callback to this proxy. */
  public void connectStructuredPullConsumer(NotificationPullConsumer consumer) {
    connectStructuredPullConsumer(consumer, MATCH_ALL, NotificationPolicies.defaults());
  }

  /** Connects the local structured pull consumer callback with a bounded filter. */
  public void connectStructuredPullConsumer(
      NotificationPullConsumer consumer, NotificationFilter filter) {
    connectStructuredPullConsumer(consumer, filter, NotificationPolicies.defaults());
  }

  /** Connects the local structured pull consumer callback with bounded filter and policies. */
  public void connectStructuredPullConsumer(
      NotificationPullConsumer consumer, NotificationFilter filter, NotificationPolicies policies) {
    requireAlive();
    channel.requireActive(this);
    if (consumer == null) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.PROXY_NOT_CONNECTED,
          "structured pull consumer must not be null");
    }
    NotificationFilter checkedFilter = requireFilter(filter);
    NotificationPolicies checkedPolicies = requirePolicies(policies);
    requireFilterWithinPolicy(checkedFilter, checkedPolicies);
    if (!this.consumer.compareAndSet(null, consumer)) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.CONNECTION_ALREADY_ACTIVE,
          "structured pull consumer is already connected");
    }
    this.filter = checkedFilter;
    this.policies = checkedPolicies;
  }

  /** Pulls one local structured event from this proxy queue or a connected pull supplier. */
  public NotificationStructuredEvent pullStructuredEvent() {
    requireAlive();
    channel.requireActive(this);
    requireConnected();
    Optional<NotificationStructuredEvent> queued = pollQueued();
    if (queued.isPresent()) {
      return queued.get();
    }
    return channel
        .pullFromSupplier()
        .filter(filter::evaluate)
        .orElseThrow(
            () ->
                new NotificationServiceException(
                    NotificationServiceDiagnosticCodes.NO_EVENT_AVAILABLE,
                    "no local Notification Service structured event is available"));
  }

  /**
   * Tries to pull one local structured event from this proxy queue or a connected pull supplier.
   */
  public Optional<NotificationStructuredEvent> tryPullStructuredEvent() {
    requireAlive();
    channel.requireActive(this);
    requireConnected();
    Optional<NotificationStructuredEvent> queued = pollQueued();
    if (queued.isPresent()) {
      return queued;
    }
    Optional<NotificationStructuredEvent> supplied = channel.tryPullFromSupplier();
    if (supplied.isPresent() && filter.evaluate(supplied.get())) {
      return supplied;
    }
    return Optional.empty();
  }

  /** Disconnects the local structured pull consumer callback if present. */
  public void disconnectStructuredPullConsumer() {
    NotificationPullConsumer connected = consumer.getAndSet(null);
    synchronized (queue) {
      queue.clear();
    }
    if (connected != null) {
      connected.disconnectStructuredPullConsumer();
    }
  }

  int proxyLimit() {
    return policies.proxyLimit();
  }

  /** Returns whether a local structured pull consumer is connected. */
  public boolean isConnected() {
    return consumer.get() != null;
  }

  void enqueue(NotificationStructuredEvent event) {
    requireAlive();
    channel.requireActive(this);
    NotificationStructuredEvent structuredEvent = LocalNotificationChannel.requireEvent(event);
    if (consumer.get() == null || !filter.evaluate(structuredEvent)) {
      return;
    }
    synchronized (queue) {
      if (queue.size() >= policies.queueLimit()) {
        throw new NotificationServiceException(
            NotificationServiceDiagnosticCodes.EVENT_QUEUE_FULL,
            "structured pull supplier queue is full: " + id());
      }
      queue.addLast(structuredEvent);
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

  private Optional<NotificationStructuredEvent> pollQueued() {
    synchronized (queue) {
      return Optional.ofNullable(queue.pollFirst());
    }
  }

  private void requireConnected() {
    if (consumer.get() == null) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.PROXY_NOT_CONNECTED,
          "structured pull consumer is not connected");
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
    disconnectStructuredPullConsumer();
  }
}
