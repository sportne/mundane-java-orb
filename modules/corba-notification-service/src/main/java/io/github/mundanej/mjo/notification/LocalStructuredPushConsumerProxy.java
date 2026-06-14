package io.github.mundanej.mjo.notification;

import java.util.concurrent.atomic.AtomicReference;

/** Local lifecycle handle for a structured push-consumer proxy. */
public final class LocalStructuredPushConsumerProxy extends LocalNotificationProxy {

  private final LocalNotificationChannel channel;
  private final AtomicReference<NotificationPushSupplier> supplier = new AtomicReference<>();

  LocalStructuredPushConsumerProxy(LocalNotificationChannel channel, long id) {
    super(channel, id, NotificationProxyKind.STRUCTURED_PUSH_CONSUMER);
    this.channel = channel;
  }

  /** Connects the local structured push supplier callback to this proxy. */
  public void connectStructuredPushSupplier(NotificationPushSupplier supplier) {
    requireAlive();
    channel.requireActive(this);
    if (supplier == null) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.PROXY_NOT_CONNECTED,
          "structured push supplier must not be null");
    }
    if (!this.supplier.compareAndSet(null, supplier)) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.CONNECTION_ALREADY_ACTIVE,
          "structured push supplier is already connected");
    }
  }

  /** Pushes one local structured event through the owning channel. */
  public void pushStructuredEvent(NotificationStructuredEvent event) {
    requireAlive();
    channel.requireActive(this);
    if (supplier.get() == null) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.PROXY_NOT_CONNECTED,
          "structured push supplier is not connected");
    }
    channel.deliverPushed(event);
  }

  /** Disconnects the local structured push supplier callback if present. */
  public void disconnectStructuredPushSupplier() {
    NotificationPushSupplier connected = supplier.getAndSet(null);
    if (connected != null) {
      connected.disconnectStructuredPushSupplier();
    }
  }

  /** Returns whether a local structured push supplier is connected. */
  public boolean isConnected() {
    return supplier.get() != null;
  }

  @Override
  void onDestroy() {
    disconnectStructuredPushSupplier();
  }
}
