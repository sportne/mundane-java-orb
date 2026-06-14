package io.github.mundanej.mjo.notification;

/** Local lifecycle handle for a structured pull-consumer proxy. */
public final class LocalStructuredPullConsumerProxy extends LocalNotificationProxy {

  LocalStructuredPullConsumerProxy(LocalNotificationChannel channel, long id) {
    super(channel, id, NotificationProxyKind.STRUCTURED_PULL_CONSUMER);
  }
}
