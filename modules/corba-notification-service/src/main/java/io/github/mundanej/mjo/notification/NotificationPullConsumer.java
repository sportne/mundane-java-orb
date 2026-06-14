package io.github.mundanej.mjo.notification;

/** Local structured pull consumer callback for the supported Notification Service subset. */
public interface NotificationPullConsumer {

  /** Notifies the consumer that its pull connection has been disconnected. */
  void disconnectStructuredPullConsumer();
}
