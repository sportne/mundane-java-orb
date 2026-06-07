package io.github.mundanej.mjo.event;

/** Local proxy that exposes events to a pull supplier. */
public final class LocalPullConsumerProxy extends LocalEventProxy {

  LocalPullConsumerProxy(LocalEventChannel channel, long id) {
    super(channel, id, EventProxyKind.PULL_CONSUMER);
  }
}
