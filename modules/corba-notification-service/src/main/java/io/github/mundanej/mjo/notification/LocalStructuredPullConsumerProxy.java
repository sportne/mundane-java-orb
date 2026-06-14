package io.github.mundanej.mjo.notification;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Local lifecycle handle for a structured pull-consumer proxy. */
public final class LocalStructuredPullConsumerProxy extends LocalNotificationProxy {

  private final LocalNotificationChannel channel;
  private final AtomicReference<NotificationPullSupplier> supplier = new AtomicReference<>();

  LocalStructuredPullConsumerProxy(LocalNotificationChannel channel, long id) {
    super(channel, id, NotificationProxyKind.STRUCTURED_PULL_CONSUMER);
    this.channel = channel;
  }

  /** Connects the local structured pull supplier callback to this proxy. */
  public void connectStructuredPullSupplier(NotificationPullSupplier supplier) {
    requireAlive();
    channel.requireActive(this);
    if (supplier == null) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.PROXY_NOT_CONNECTED,
          "structured pull supplier must not be null");
    }
    if (!this.supplier.compareAndSet(null, supplier)) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.CONNECTION_ALREADY_ACTIVE,
          "structured pull supplier is already connected");
    }
    channel.registerPullConsumer(this);
  }

  /** Disconnects the local structured pull supplier callback if present. */
  public void disconnectStructuredPullSupplier() {
    NotificationPullSupplier connected = supplier.getAndSet(null);
    channel.unregisterPullConsumer(this);
    if (connected != null) {
      connected.disconnectStructuredPullSupplier();
    }
  }

  /** Returns whether a local structured pull supplier is connected. */
  public boolean isConnected() {
    return supplier.get() != null;
  }

  Optional<NotificationStructuredEvent> tryPullFromSupplier() {
    requireAlive();
    channel.requireActive(this);
    NotificationPullSupplier connected = supplier.get();
    if (connected == null) {
      return Optional.empty();
    }
    return requireSupplierResult(connected.tryPullStructuredEvent());
  }

  Optional<NotificationStructuredEvent> pullFromSupplier() {
    requireAlive();
    channel.requireActive(this);
    NotificationPullSupplier connected = supplier.get();
    if (connected == null) {
      return Optional.empty();
    }
    return requireSupplierResult(connected.pullStructuredEvent());
  }

  private Optional<NotificationStructuredEvent> requireSupplierResult(
      Optional<NotificationStructuredEvent> event) {
    if (event == null) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.MALFORMED_STRUCTURED_EVENT,
          "structured pull supplier returned a null result");
    }
    return event.map(LocalNotificationChannel::requireEvent);
  }

  @Override
  void onDestroy() {
    disconnectStructuredPullSupplier();
  }
}
