package io.github.mundanej.mjo.notification;

import io.github.mundanej.mjo.event.EventProxyKind;

/** Local Notification Service proxy role and its Event Service compatibility role. */
public enum NotificationProxyKind {
  /** Proxy that accepts structured push events from a supplier. */
  STRUCTURED_PUSH_CONSUMER(EventProxyKind.PUSH_CONSUMER, true),

  /** Proxy that accepts structured pull events from a supplier. */
  STRUCTURED_PULL_CONSUMER(EventProxyKind.PULL_CONSUMER, true),

  /** Proxy that supplies structured push events to a consumer. */
  STRUCTURED_PUSH_SUPPLIER(EventProxyKind.PUSH_SUPPLIER, false),

  /** Proxy that supplies structured pull events to a consumer. */
  STRUCTURED_PULL_SUPPLIER(EventProxyKind.PULL_SUPPLIER, false);

  private final EventProxyKind eventProxyKind;
  private final boolean supplierSide;

  NotificationProxyKind(EventProxyKind eventProxyKind, boolean supplierSide) {
    this.eventProxyKind = eventProxyKind;
    this.supplierSide = supplierSide;
  }

  /** Returns the compatible Event Service proxy role for this Notification proxy role. */
  public EventProxyKind eventProxyKind() {
    return eventProxyKind;
  }

  /** Returns true for supplier-admin-owned proxy roles. */
  public boolean isSupplierSide() {
    return supplierSide;
  }
}
