package io.github.mundanej.mjo.event;

import io.github.mundanej.mjo.any.AnyValue;
import java.util.concurrent.atomic.AtomicReference;

/** Local proxy that accepts pushed events from a push supplier. */
public final class LocalPushConsumerProxy extends LocalEventProxy {

  private final LocalEventChannel channel;
  private final AtomicReference<EventPushSupplier> supplier = new AtomicReference<>();

  LocalPushConsumerProxy(LocalEventChannel channel, long id) {
    super(channel, id, EventProxyKind.PUSH_CONSUMER);
    this.channel = channel;
  }

  /** Connects the local push supplier callback to this proxy. */
  public void connectPushSupplier(EventPushSupplier supplier) {
    requireAlive();
    channel.requireActive(this);
    if (supplier == null) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.PROXY_NOT_CONNECTED, "push supplier must not be null");
    }
    if (!this.supplier.compareAndSet(null, supplier)) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.CONNECTION_ALREADY_ACTIVE,
          "push supplier is already connected");
    }
  }

  /** Pushes one local event payload through the owning channel. */
  public void push(AnyValue<?> event) {
    requireAlive();
    channel.requireActive(this);
    if (supplier.get() == null) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.PROXY_NOT_CONNECTED, "push supplier is not connected");
    }
    channel.deliverPushed(event);
  }

  /** Disconnects the local push supplier callback if present. */
  public void disconnectPushSupplier() {
    EventPushSupplier connected = supplier.getAndSet(null);
    if (connected != null) {
      connected.disconnectPushSupplier();
    }
  }

  /** Returns whether a local push supplier is connected. */
  public boolean isConnected() {
    return supplier.get() != null;
  }

  @Override
  void onDestroy() {
    disconnectPushSupplier();
  }
}
