package io.github.mundanej.mjo.event;

import io.github.mundanej.mjo.any.AnyValue;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Local proxy that exposes events to a pull supplier. */
public final class LocalPullConsumerProxy extends LocalEventProxy {

  private final LocalEventChannel channel;
  private final AtomicReference<EventPullSupplier> supplier = new AtomicReference<>();

  LocalPullConsumerProxy(LocalEventChannel channel, long id) {
    super(channel, id, EventProxyKind.PULL_CONSUMER);
    this.channel = channel;
  }

  /** Connects the local pull supplier callback to this proxy. */
  public void connectPullSupplier(EventPullSupplier supplier) {
    requireAlive();
    channel.requireActive(this);
    if (supplier == null) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.PROXY_NOT_CONNECTED, "pull supplier must not be null");
    }
    if (!this.supplier.compareAndSet(null, supplier)) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.CONNECTION_ALREADY_ACTIVE,
          "pull supplier is already connected");
    }
    channel.registerPullConsumer(this);
  }

  /** Disconnects the local pull supplier callback if present. */
  public void disconnectPullSupplier() {
    EventPullSupplier connected = supplier.getAndSet(null);
    channel.unregisterPullConsumer(this);
    if (connected != null) {
      connected.disconnectPullSupplier();
    }
  }

  /** Returns whether a local pull supplier is connected. */
  public boolean isConnected() {
    return supplier.get() != null;
  }

  Optional<AnyValue<?>> tryPullFromSupplier() {
    requireAlive();
    channel.requireActive(this);
    EventPullSupplier connected = supplier.get();
    if (connected == null) {
      return Optional.empty();
    }
    return requireSupplierResult(connected.tryPull());
  }

  Optional<AnyValue<?>> pullFromSupplier() {
    requireAlive();
    channel.requireActive(this);
    EventPullSupplier connected = supplier.get();
    if (connected == null) {
      return Optional.empty();
    }
    return requireSupplierResult(connected.pull());
  }

  private Optional<AnyValue<?>> requireSupplierResult(Optional<AnyValue<?>> event) {
    if (event == null) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.INVALID_PAYLOAD, "pull supplier returned a null result");
    }
    return event.map(LocalEventChannel::requirePayload);
  }

  @Override
  void onDestroy() {
    disconnectPullSupplier();
  }
}
