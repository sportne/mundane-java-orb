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
  private final List<LocalPushSupplierProxy> pushSuppliers = new CopyOnWriteArrayList<>();
  private final List<LocalPullConsumerProxy> pullConsumers = new CopyOnWriteArrayList<>();
  private long nextProxyId = 1L;
  private boolean destroyed;

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

  LocalEventProxy createProxy(EventProxyKind kind) {
    requireAlive();
    long proxyId = nextProxyId++;
    return switch (Objects.requireNonNull(kind, "kind")) {
      case PUSH_CONSUMER -> new LocalPushConsumerProxy(this, proxyId);
      case PULL_CONSUMER -> new LocalPullConsumerProxy(this, proxyId);
      case PUSH_SUPPLIER -> {
        LocalPushSupplierProxy proxy = new LocalPushSupplierProxy(this, proxyId);
        pushSuppliers.add(proxy);
        yield proxy;
      }
      case PULL_SUPPLIER -> new LocalPullSupplierProxy(this, proxyId);
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
    for (LocalPushSupplierProxy proxy : List.copyOf(pushSuppliers)) {
      proxy.deliver(payload);
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

  void requireAlive() {
    service.requireOpen();
    if (destroyed) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.CHANNEL_DESTROYED, "event channel is destroyed: " + id);
    }
  }
}
