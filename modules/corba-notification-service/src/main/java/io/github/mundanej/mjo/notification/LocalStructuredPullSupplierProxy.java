package io.github.mundanej.mjo.notification;

/** Local lifecycle handle for a structured pull-supplier proxy. */
public final class LocalStructuredPullSupplierProxy extends LocalNotificationProxy {

  LocalStructuredPullSupplierProxy(LocalNotificationChannel channel, long id) {
    super(channel, id, NotificationProxyKind.STRUCTURED_PULL_SUPPLIER);
  }
}
