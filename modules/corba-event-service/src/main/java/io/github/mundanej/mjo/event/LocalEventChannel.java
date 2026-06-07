package io.github.mundanej.mjo.event;

import java.util.Objects;

/** Local Event Service channel lifecycle object. */
public final class LocalEventChannel {

  private final LocalEventService service;
  private final long id;
  private final LocalEventSupplierAdmin supplierAdmin;
  private final LocalEventConsumerAdmin consumerAdmin;
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
      case PUSH_SUPPLIER -> new LocalPushSupplierProxy(this, proxyId);
      case PULL_SUPPLIER -> new LocalPullSupplierProxy(this, proxyId);
    };
  }

  void requireAlive() {
    service.requireOpen();
    if (destroyed) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.CHANNEL_DESTROYED, "event channel is destroyed: " + id);
    }
  }
}
