package io.github.mundanej.mjo.event;

/** Local pull consumer callback used by later delivery slices. */
public interface EventPullConsumer {

  /** Notifies the consumer that its pull connection has been disconnected. */
  void disconnectPullConsumer();
}
