package io.github.mundanej.mjo.event;

import java.util.Objects;

/** Local Event Service supplier admin surface. */
public final class LocalEventSupplierAdmin {

  private final LocalEventChannel channel;

  LocalEventSupplierAdmin(LocalEventChannel channel) {
    this.channel = Objects.requireNonNull(channel, "channel");
  }

  /** Returns the local channel id that owns this admin. */
  public long channelId() {
    return channel.id();
  }

  /** Creates a proxy that accepts pushed events from a push supplier. */
  public LocalPushConsumerProxy obtainPushConsumerProxy() {
    return (LocalPushConsumerProxy) channel.createProxy(EventProxyKind.PUSH_CONSUMER);
  }

  /** Creates a proxy that exposes events to a pull supplier. */
  public LocalPullConsumerProxy obtainPullConsumerProxy() {
    return (LocalPullConsumerProxy) channel.createProxy(EventProxyKind.PULL_CONSUMER);
  }
}
