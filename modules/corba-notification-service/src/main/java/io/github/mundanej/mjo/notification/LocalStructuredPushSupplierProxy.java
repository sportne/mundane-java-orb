package io.github.mundanej.mjo.notification;

/** Local lifecycle handle for a structured push-supplier proxy. */
public final class LocalStructuredPushSupplierProxy extends LocalNotificationProxy {

  LocalStructuredPushSupplierProxy(LocalNotificationChannel channel, long id) {
    super(channel, id, NotificationProxyKind.STRUCTURED_PUSH_SUPPLIER);
  }
}
