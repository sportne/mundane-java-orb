package io.github.mundanej.mjo.event;

/** Local proxy that accepts pushed events from a push supplier. */
public final class LocalPushConsumerProxy extends LocalEventProxy {

  LocalPushConsumerProxy(LocalEventChannel channel, long id) {
    super(channel, id, EventProxyKind.PUSH_CONSUMER);
  }
}
