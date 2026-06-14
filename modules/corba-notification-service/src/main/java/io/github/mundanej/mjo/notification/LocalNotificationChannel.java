package io.github.mundanej.mjo.notification;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/** Local Notification Service channel lifecycle object. */
public final class LocalNotificationChannel {

  private final LocalNotificationService service;
  private final long id;
  private final LocalNotificationSupplierAdmin supplierAdmin;
  private final LocalNotificationConsumerAdmin consumerAdmin;
  private final List<LocalNotificationProxy> supplierSideProxies = new CopyOnWriteArrayList<>();
  private final List<LocalNotificationProxy> consumerSideProxies = new CopyOnWriteArrayList<>();
  private final List<LocalStructuredPushSupplierProxy> pushSuppliers = new CopyOnWriteArrayList<>();
  private final List<LocalStructuredPullSupplierProxy> pullSuppliers = new CopyOnWriteArrayList<>();
  private final List<LocalStructuredPullConsumerProxy> pullConsumers = new CopyOnWriteArrayList<>();
  private long nextProxyId = 1L;
  private volatile boolean destroyed;

  LocalNotificationChannel(LocalNotificationService service, long id) {
    this.service = Objects.requireNonNull(service, "service");
    this.id = id;
    supplierAdmin = new LocalNotificationSupplierAdmin(this);
    consumerAdmin = new LocalNotificationConsumerAdmin(this);
  }

  /** Returns this channel's local service-scoped id. */
  public long id() {
    return id;
  }

  /** Returns this channel's local Event Service compatibility boundary identity. */
  public NotificationEventCompatibility eventCompatibility() {
    requireAlive();
    return service.eventCompatibility();
  }

  /** Returns the supplier admin for supplier-side proxy creation. */
  public LocalNotificationSupplierAdmin supplierAdmin() {
    requireAlive();
    return supplierAdmin;
  }

  /** Returns the consumer admin for consumer-side proxy creation. */
  public LocalNotificationConsumerAdmin consumerAdmin() {
    requireAlive();
    return consumerAdmin;
  }

  /** Destroys this channel and invalidates all future admin/proxy operations. */
  public void destroy() {
    requireAlive();
    service.destroyChannel(this);
    destroyed = true;
  }

  /** Returns whether this local channel has been destroyed. */
  public boolean isDestroyed() {
    return destroyed;
  }

  synchronized LocalNotificationProxy createProxy(NotificationProxyKind kind) {
    requireAlive();
    long proxyId = nextProxyId++;
    return switch (Objects.requireNonNull(kind, "kind")) {
      case STRUCTURED_PUSH_CONSUMER -> {
        requireSupplierCapacity();
        LocalStructuredPushConsumerProxy proxy =
            new LocalStructuredPushConsumerProxy(this, proxyId);
        supplierSideProxies.add(proxy);
        yield proxy;
      }
      case STRUCTURED_PULL_CONSUMER -> {
        requireSupplierCapacity();
        LocalStructuredPullConsumerProxy proxy =
            new LocalStructuredPullConsumerProxy(this, proxyId);
        supplierSideProxies.add(proxy);
        yield proxy;
      }
      case STRUCTURED_PUSH_SUPPLIER -> {
        requireConsumerCapacity();
        LocalStructuredPushSupplierProxy proxy =
            new LocalStructuredPushSupplierProxy(this, proxyId);
        consumerSideProxies.add(proxy);
        pushSuppliers.add(proxy);
        yield proxy;
      }
      case STRUCTURED_PULL_SUPPLIER -> {
        requireConsumerCapacity();
        LocalStructuredPullSupplierProxy proxy =
            new LocalStructuredPullSupplierProxy(this, proxyId);
        consumerSideProxies.add(proxy);
        pullSuppliers.add(proxy);
        yield proxy;
      }
    };
  }

  void registerPullConsumer(LocalStructuredPullConsumerProxy proxy) {
    requireAlive();
    if (!pullConsumers.contains(proxy)) {
      pullConsumers.add(proxy);
    }
  }

  void unregisterPullConsumer(LocalStructuredPullConsumerProxy proxy) {
    pullConsumers.remove(proxy);
  }

  void deliverPushed(NotificationStructuredEvent event) {
    requireAlive();
    NotificationStructuredEvent structuredEvent = requireEvent(event);
    List<LocalStructuredPushSupplierProxy> pushTargets =
        pushSuppliers.stream().filter(LocalStructuredPushSupplierProxy::isConnected).toList();
    List<LocalStructuredPullSupplierProxy> pullTargets =
        pullSuppliers.stream().filter(LocalStructuredPullSupplierProxy::isConnected).toList();
    int targetCount = pushTargets.size() + pullTargets.size();
    requireFanoutCapacity(targetCount, effectiveProxyLimit(pushTargets, pullTargets));

    NotificationServiceException firstFailure = null;
    for (LocalStructuredPullSupplierProxy proxy : pullTargets) {
      try {
        proxy.enqueue(structuredEvent);
      } catch (NotificationServiceException exception) {
        if (firstFailure == null) {
          firstFailure = exception;
        }
      }
    }
    for (LocalStructuredPushSupplierProxy proxy : pushTargets) {
      try {
        proxy.deliver(structuredEvent);
      } catch (NotificationServiceException exception) {
        if (firstFailure == null) {
          firstFailure = exception;
        }
      } catch (RuntimeException exception) {
        proxy.markFailed();
        removeProxy(proxy);
        NotificationServiceException failure =
            new NotificationServiceException(
                NotificationServiceDiagnosticCodes.CONSUMER_DELIVERY_FAILED,
                "push consumer failed during local Notification Service delivery: "
                    + exception.getClass().getName());
        if (firstFailure == null) {
          firstFailure = failure;
        }
      }
    }
    if (firstFailure != null) {
      throw firstFailure;
    }
  }

  Optional<NotificationStructuredEvent> tryPullFromSupplier() {
    requireAlive();
    for (LocalStructuredPullConsumerProxy proxy : List.copyOf(pullConsumers)) {
      Optional<NotificationStructuredEvent> event = proxy.tryPullFromSupplier();
      if (event.isPresent()) {
        return event;
      }
    }
    return Optional.empty();
  }

  Optional<NotificationStructuredEvent> pullFromSupplier() {
    requireAlive();
    for (LocalStructuredPullConsumerProxy proxy : List.copyOf(pullConsumers)) {
      Optional<NotificationStructuredEvent> event = proxy.pullFromSupplier();
      if (event.isPresent()) {
        return event;
      }
    }
    return Optional.empty();
  }

  NotificationStructuredEvent requirePulledEvent() {
    return pullFromSupplier()
        .orElseThrow(
            () ->
                new NotificationServiceException(
                    NotificationServiceDiagnosticCodes.NO_EVENT_AVAILABLE,
                    "no local Notification Service structured event is available"));
  }

  static NotificationStructuredEvent requireEvent(NotificationStructuredEvent event) {
    return NotificationStructuredEvent.requirePresent("structured event", event);
  }

  void requireActive(LocalNotificationProxy proxy) {
    requireAlive();
    boolean active =
        switch (proxy.kind()) {
          case STRUCTURED_PUSH_CONSUMER, STRUCTURED_PULL_CONSUMER ->
              supplierSideProxies.contains(proxy);
          case STRUCTURED_PUSH_SUPPLIER, STRUCTURED_PULL_SUPPLIER ->
              consumerSideProxies.contains(proxy);
        };
    if (!active) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.STALE_PROXY,
          "notification proxy is stale: " + proxy.id());
    }
  }

  void removeProxy(LocalNotificationProxy proxy) {
    supplierSideProxies.remove(proxy);
    consumerSideProxies.remove(proxy);
    pushSuppliers.remove(proxy);
    pullSuppliers.remove(proxy);
    pullConsumers.remove(proxy);
  }

  void requireAlive() {
    service.requireOpen();
    if (destroyed) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.CHANNEL_DESTROYED,
          "notification channel is destroyed: " + id);
    }
  }

  private void requireSupplierCapacity() {
    if (supplierSideProxies.size() >= service.options().maxSuppliersPerChannel()) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.SUPPLIER_LIMIT_EXCEEDED,
          "maximum supplier-side notification proxy count reached for channel: " + id);
    }
  }

  private void requireConsumerCapacity() {
    if (consumerSideProxies.size() >= service.options().maxConsumersPerChannel()) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.CONSUMER_LIMIT_EXCEEDED,
          "maximum consumer-side notification proxy count reached for channel: " + id);
    }
  }

  private int effectiveProxyLimit(
      List<LocalStructuredPushSupplierProxy> pushTargets,
      List<LocalStructuredPullSupplierProxy> pullTargets) {
    int limit = NotificationPolicies.defaults().proxyLimit();
    for (LocalStructuredPushSupplierProxy proxy : pushTargets) {
      limit = Math.min(limit, proxy.proxyLimit());
    }
    for (LocalStructuredPullSupplierProxy proxy : pullTargets) {
      limit = Math.min(limit, proxy.proxyLimit());
    }
    return limit;
  }

  private void requireFanoutCapacity(int targets, int limit) {
    if (targets > limit) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.EVENT_QUEUE_FULL,
          "local Notification Service fan-out capacity exceeded for channel: " + id);
    }
  }
}
