package io.github.mundanej.mjo.notification;

import java.util.Objects;

/** Local Notification Service supplier admin surface. */
public final class LocalNotificationSupplierAdmin {

  private final LocalNotificationChannel channel;

  LocalNotificationSupplierAdmin(LocalNotificationChannel channel) {
    this.channel = Objects.requireNonNull(channel, "channel");
  }

  /** Returns the local channel id that owns this admin. */
  public long channelId() {
    return channel.id();
  }

  /** Creates a proxy that accepts structured pushed events from a push supplier. */
  public LocalStructuredPushConsumerProxy obtainStructuredPushConsumerProxy() {
    return (LocalStructuredPushConsumerProxy)
        channel.createProxy(NotificationProxyKind.STRUCTURED_PUSH_CONSUMER);
  }

  /** Creates a proxy that exposes structured events to a pull supplier. */
  public LocalStructuredPullConsumerProxy obtainStructuredPullConsumerProxy() {
    return (LocalStructuredPullConsumerProxy)
        channel.createProxy(NotificationProxyKind.STRUCTURED_PULL_CONSUMER);
  }
}
