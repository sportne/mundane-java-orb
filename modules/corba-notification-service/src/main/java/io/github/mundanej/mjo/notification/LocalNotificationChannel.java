package io.github.mundanej.mjo.notification;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Local Notification Service channel lifecycle object. */
public final class LocalNotificationChannel {

  private final LocalNotificationService service;
  private final long id;
  private final LocalNotificationSupplierAdmin supplierAdmin;
  private final LocalNotificationConsumerAdmin consumerAdmin;
  private final List<LocalNotificationProxy> supplierSideProxies = new CopyOnWriteArrayList<>();
  private final List<LocalNotificationProxy> consumerSideProxies = new CopyOnWriteArrayList<>();
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
        yield proxy;
      }
      case STRUCTURED_PULL_SUPPLIER -> {
        requireConsumerCapacity();
        LocalStructuredPullSupplierProxy proxy =
            new LocalStructuredPullSupplierProxy(this, proxyId);
        consumerSideProxies.add(proxy);
        yield proxy;
      }
    };
  }

  void removeProxy(LocalNotificationProxy proxy) {
    supplierSideProxies.remove(proxy);
    consumerSideProxies.remove(proxy);
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
}
