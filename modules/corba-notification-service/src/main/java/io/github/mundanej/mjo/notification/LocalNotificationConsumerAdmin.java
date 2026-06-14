package io.github.mundanej.mjo.notification;

import java.util.Objects;

/** Local Notification Service consumer admin surface. */
public final class LocalNotificationConsumerAdmin {

  private final LocalNotificationChannel channel;

  LocalNotificationConsumerAdmin(LocalNotificationChannel channel) {
    this.channel = Objects.requireNonNull(channel, "channel");
  }

  /** Returns the local channel id that owns this admin. */
  public long channelId() {
    return channel.id();
  }

  /** Creates a proxy that supplies structured pushed events to a push consumer. */
  public LocalStructuredPushSupplierProxy obtainStructuredPushSupplierProxy() {
    return (LocalStructuredPushSupplierProxy)
        channel.createProxy(NotificationProxyKind.STRUCTURED_PUSH_SUPPLIER);
  }

  /** Creates a proxy that supplies structured events to a pull consumer. */
  public LocalStructuredPullSupplierProxy obtainStructuredPullSupplierProxy() {
    return (LocalStructuredPullSupplierProxy)
        channel.createProxy(NotificationProxyKind.STRUCTURED_PULL_SUPPLIER);
  }
}
