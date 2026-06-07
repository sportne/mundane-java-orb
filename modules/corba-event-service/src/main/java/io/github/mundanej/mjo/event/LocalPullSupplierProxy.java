package io.github.mundanej.mjo.event;

import io.github.mundanej.mjo.any.AnyValue;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Local proxy that supplies events to a pull consumer. */
public final class LocalPullSupplierProxy extends LocalEventProxy {

  private final LocalEventChannel channel;
  private final AtomicReference<EventPullConsumer> consumer = new AtomicReference<>();

  LocalPullSupplierProxy(LocalEventChannel channel, long id) {
    super(channel, id, EventProxyKind.PULL_SUPPLIER);
    this.channel = channel;
  }

  /** Connects the local pull consumer callback to this proxy. */
  public void connectPullConsumer(EventPullConsumer consumer) {
    requireAlive();
    channel.requireActive(this);
    if (consumer == null) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.PROXY_NOT_CONNECTED, "pull consumer must not be null");
    }
    if (!this.consumer.compareAndSet(null, consumer)) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.CONNECTION_ALREADY_ACTIVE,
          "pull consumer is already connected");
    }
  }

  /** Pulls one local event payload from a connected pull supplier. */
  public AnyValue<?> pull() {
    requireAlive();
    channel.requireActive(this);
    requireConnected();
    return channel.requirePulledEvent();
  }

  /** Tries to pull one local event payload from a connected pull supplier. */
  public Optional<AnyValue<?>> tryPull() {
    requireAlive();
    channel.requireActive(this);
    requireConnected();
    return channel.tryPullFromSupplier();
  }

  /** Disconnects the local pull consumer callback if present. */
  public void disconnectPullConsumer() {
    EventPullConsumer connected = consumer.getAndSet(null);
    if (connected != null) {
      connected.disconnectPullConsumer();
    }
  }

  /** Returns whether a local pull consumer is connected. */
  public boolean isConnected() {
    return consumer.get() != null;
  }

  private void requireConnected() {
    if (consumer.get() == null) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.PROXY_NOT_CONNECTED, "pull consumer is not connected");
    }
  }

  @Override
  void onDestroy() {
    disconnectPullConsumer();
  }
}
