package io.github.mundanej.mjo.event;

/** Local proxy that supplies events to a pull consumer. */
public final class LocalPullSupplierProxy extends LocalEventProxy {

  LocalPullSupplierProxy(LocalEventChannel channel, long id) {
    super(channel, id, EventProxyKind.PULL_SUPPLIER);
  }
}
