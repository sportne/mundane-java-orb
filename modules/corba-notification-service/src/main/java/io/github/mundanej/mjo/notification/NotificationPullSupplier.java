package io.github.mundanej.mjo.notification;

import java.util.Optional;

/** Local structured pull supplier callback for the supported Notification Service subset. */
public interface NotificationPullSupplier {

  /** Pulls one structured event, blocking only according to caller policy. */
  Optional<NotificationStructuredEvent> pullStructuredEvent();

  /** Tries to pull one structured event without waiting for new data. */
  Optional<NotificationStructuredEvent> tryPullStructuredEvent();

  /** Notifies the supplier that its pull connection has been disconnected. */
  void disconnectStructuredPullSupplier();
}
