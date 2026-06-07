package io.github.mundanej.mjo.event;

import io.github.mundanej.mjo.any.AnyValue;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/** Local Event Service channel lifecycle object. */
public final class LocalEventChannel {

  private final LocalEventService service;
  private final long id;
  private final LocalEventSupplierAdmin supplierAdmin;
  private final LocalEventConsumerAdmin consumerAdmin;
  private final List<LocalEventProxy> supplierSideProxies = new CopyOnWriteArrayList<>();
  private final List<LocalEventProxy> consumerSideProxies = new CopyOnWriteArrayList<>();
  private final List<LocalPushSupplierProxy> pushSuppliers = new CopyOnWriteArrayList<>();
  private final List<LocalPullConsumerProxy> pullConsumers = new CopyOnWriteArrayList<>();
  private long nextProxyId = 1L;
  private volatile boolean destroyed;

  LocalEventChannel(LocalEventService service, long id) {
    this.service = Objects.requireNonNull(service, "service");
    this.id = id;
    supplierAdmin = new LocalEventSupplierAdmin(this);
    consumerAdmin = new LocalEventConsumerAdmin(this);
  }

  /** Returns this channel's local service-scoped id. */
  public long id() {
    return id;
  }

  /** Returns the supplier admin for supplier-side proxy creation. */
  public LocalEventSupplierAdmin supplierAdmin() {
    requireAlive();
    return supplierAdmin;
  }

  /** Returns the consumer admin for consumer-side proxy creation. */
  public LocalEventConsumerAdmin consumerAdmin() {
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

  synchronized LocalEventProxy createProxy(EventProxyKind kind) {
    requireAlive();
    long proxyId = nextProxyId++;
    return switch (Objects.requireNonNull(kind, "kind")) {
      case PUSH_CONSUMER -> {
        requireSupplierCapacity();
        LocalPushConsumerProxy proxy = new LocalPushConsumerProxy(this, proxyId);
        supplierSideProxies.add(proxy);
        yield proxy;
      }
      case PULL_CONSUMER -> {
        requireSupplierCapacity();
        LocalPullConsumerProxy proxy = new LocalPullConsumerProxy(this, proxyId);
        supplierSideProxies.add(proxy);
        yield proxy;
      }
      case PUSH_SUPPLIER -> {
        requireConsumerCapacity();
        LocalPushSupplierProxy proxy = new LocalPushSupplierProxy(this, proxyId);
        consumerSideProxies.add(proxy);
        pushSuppliers.add(proxy);
        yield proxy;
      }
      case PULL_SUPPLIER -> {
        requireConsumerCapacity();
        LocalPullSupplierProxy proxy = new LocalPullSupplierProxy(this, proxyId);
        consumerSideProxies.add(proxy);
        yield proxy;
      }
    };
  }

  void registerPullConsumer(LocalPullConsumerProxy proxy) {
    requireAlive();
    if (!pullConsumers.contains(proxy)) {
      pullConsumers.add(proxy);
    }
  }

  void unregisterPullConsumer(LocalPullConsumerProxy proxy) {
    pullConsumers.remove(proxy);
  }

  void deliverPushed(AnyValue<?> event) {
    requireAlive();
    AnyValue<?> payload = requirePayload(event);
    List<LocalPushSupplierProxy> targets =
        pushSuppliers.stream().filter(LocalPushSupplierProxy::isConnected).toList();
    requirePendingCapacity(targets.size());
    EventServiceException firstFailure = null;
    for (LocalPushSupplierProxy proxy : targets) {
      try {
        proxy.deliver(payload);
      } catch (RuntimeException exception) {
        proxy.markFailed();
        removeProxy(proxy);
        EventServiceException failure =
            new EventServiceException(
                EventServiceDiagnosticCodes.CONSUMER_DELIVERY_FAILED,
                "push consumer failed during local Event Service delivery",
                exception);
        if (firstFailure == null) {
          firstFailure = failure;
        }
      }
    }
    if (firstFailure != null) {
      throw firstFailure;
    }
  }

  Optional<AnyValue<?>> tryPullFromSupplier() {
    requireAlive();
    for (LocalPullConsumerProxy proxy : List.copyOf(pullConsumers)) {
      Optional<AnyValue<?>> event = proxy.tryPullFromSupplier();
      if (event.isPresent()) {
        return event;
      }
    }
    return Optional.empty();
  }

  Optional<AnyValue<?>> pullFromSupplier() {
    requireAlive();
    for (LocalPullConsumerProxy proxy : List.copyOf(pullConsumers)) {
      Optional<AnyValue<?>> event = proxy.pullFromSupplier();
      if (event.isPresent()) {
        return event;
      }
    }
    return Optional.empty();
  }

  AnyValue<?> requirePulledEvent() {
    return pullFromSupplier()
        .orElseThrow(
            () ->
                new EventServiceException(
                    EventServiceDiagnosticCodes.NO_EVENT_AVAILABLE,
                    "no local Event Service payload is available"));
  }

  static AnyValue<?> requirePayload(AnyValue<?> event) {
    if (event == null) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.INVALID_PAYLOAD, "event payload must not be null");
    }
    return event;
  }

  void requireActive(LocalEventProxy proxy) {
    requireAlive();
    boolean active =
        switch (proxy.kind()) {
          case PUSH_CONSUMER, PULL_CONSUMER -> supplierSideProxies.contains(proxy);
          case PUSH_SUPPLIER, PULL_SUPPLIER -> consumerSideProxies.contains(proxy);
        };
    if (!active) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.STALE_PROXY, "event proxy is stale: " + proxy.id());
    }
  }

  void removeProxy(LocalEventProxy proxy) {
    supplierSideProxies.remove(proxy);
    consumerSideProxies.remove(proxy);
    pushSuppliers.remove(proxy);
    pullConsumers.remove(proxy);
  }

  private void requireSupplierCapacity() {
    if (supplierSideProxies.size() >= service.options().maxSuppliersPerChannel()) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.SUPPLIER_LIMIT_EXCEEDED,
          "maximum supplier-side proxy count reached for channel: " + id);
    }
  }

  private void requireConsumerCapacity() {
    if (consumerSideProxies.size() >= service.options().maxConsumersPerChannel()) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.CONSUMER_LIMIT_EXCEEDED,
          "maximum consumer-side proxy count reached for channel: " + id);
    }
  }

  private void requirePendingCapacity(int pendingEvents) {
    if (pendingEvents > service.options().maxPendingEvents()) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.EVENT_QUEUE_FULL,
          "local Event Service pending event capacity exceeded for channel: " + id);
    }
  }

  void requireAlive() {
    service.requireOpen();
    if (destroyed) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.CHANNEL_DESTROYED, "event channel is destroyed: " + id);
    }
  }
}
