package io.github.mundanej.mjo.event;

import java.util.Objects;

/** Local Event Service consumer admin surface. */
public final class LocalEventConsumerAdmin {

  private final LocalEventChannel channel;

  LocalEventConsumerAdmin(LocalEventChannel channel) {
    this.channel = Objects.requireNonNull(channel, "channel");
  }

  /** Returns the local channel id that owns this admin. */
  public long channelId() {
    return channel.id();
  }

  /** Creates a proxy that pushes events to a push consumer. */
  public LocalPushSupplierProxy obtainPushSupplierProxy() {
    return (LocalPushSupplierProxy) channel.createProxy(EventProxyKind.PUSH_SUPPLIER);
  }

  /** Creates a proxy that supplies events to a pull consumer. */
  public LocalPullSupplierProxy obtainPullSupplierProxy() {
    return (LocalPullSupplierProxy) channel.createProxy(EventProxyKind.PULL_SUPPLIER);
  }
}
