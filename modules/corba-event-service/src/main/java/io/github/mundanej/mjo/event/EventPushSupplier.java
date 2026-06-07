package io.github.mundanej.mjo.event;

/** Local push supplier callback used by later delivery slices. */
public interface EventPushSupplier {

  /** Notifies the supplier that its push connection has been disconnected. */
  void disconnectPushSupplier();
}
