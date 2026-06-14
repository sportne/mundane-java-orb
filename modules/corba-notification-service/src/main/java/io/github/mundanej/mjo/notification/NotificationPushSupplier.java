package io.github.mundanej.mjo.notification;

/** Local structured push supplier callback for the supported Notification Service subset. */
public interface NotificationPushSupplier {

  /** Notifies the supplier that its push connection has been disconnected. */
  void disconnectStructuredPushSupplier();
}
