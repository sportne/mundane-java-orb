package io.github.mundanej.mjo.event;

/** Local proxy that pushes events to a push consumer. */
public final class LocalPushSupplierProxy extends LocalEventProxy {

  LocalPushSupplierProxy(LocalEventChannel channel, long id) {
    super(channel, id, EventProxyKind.PUSH_SUPPLIER);
  }
}
