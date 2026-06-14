package io.github.mundanej.mjo.notification;

/** Local structured push consumer callback for the supported Notification Service subset. */
public interface NotificationPushConsumer {

  /** Receives one structured Notification Service event. */
  void pushStructuredEvent(NotificationStructuredEvent event);

  /** Notifies the consumer that its push connection has been disconnected. */
  void disconnectStructuredPushConsumer();
}
