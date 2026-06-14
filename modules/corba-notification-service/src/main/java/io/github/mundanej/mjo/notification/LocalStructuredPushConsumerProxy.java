package io.github.mundanej.mjo.notification;

/** Local lifecycle handle for a structured push-consumer proxy. */
public final class LocalStructuredPushConsumerProxy extends LocalNotificationProxy {

  LocalStructuredPushConsumerProxy(LocalNotificationChannel channel, long id) {
    super(channel, id, NotificationProxyKind.STRUCTURED_PUSH_CONSUMER);
  }
}
