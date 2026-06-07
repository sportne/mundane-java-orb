package io.github.mundanej.mjo.event;

import io.github.mundanej.mjo.any.AnyValue;
import java.util.concurrent.atomic.AtomicReference;

/** Local proxy that pushes events to a push consumer. */
public final class LocalPushSupplierProxy extends LocalEventProxy {

  private final AtomicReference<EventPushConsumer> consumer = new AtomicReference<>();

  LocalPushSupplierProxy(LocalEventChannel channel, long id) {
    super(channel, id, EventProxyKind.PUSH_SUPPLIER);
  }

  /** Connects the local push consumer callback to this proxy. */
  public void connectPushConsumer(EventPushConsumer consumer) {
    requireAlive();
    if (consumer == null) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.PROXY_NOT_CONNECTED, "push consumer must not be null");
    }
    if (!this.consumer.compareAndSet(null, consumer)) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.CONNECTION_ALREADY_ACTIVE,
          "push consumer is already connected");
    }
  }

  /** Disconnects the local push consumer callback if present. */
  public void disconnectPushConsumer() {
    EventPushConsumer connected = consumer.getAndSet(null);
    if (connected != null) {
      connected.disconnectPushConsumer();
    }
  }

  /** Returns whether a local push consumer is connected. */
  public boolean isConnected() {
    return consumer.get() != null;
  }

  void deliver(AnyValue<?> event) {
    requireAlive();
    EventPushConsumer connected = consumer.get();
    if (connected != null) {
      connected.push(LocalEventChannel.requirePayload(event));
    }
  }

  @Override
  void onDestroy() {
    disconnectPushConsumer();
  }
}
