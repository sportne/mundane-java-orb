package io.github.mundanej.mjo.event;

import io.github.mundanej.mjo.any.AnyValue;

/** Local push consumer callback used by later delivery slices. */
public interface EventPushConsumer {

  /** Receives one Event Service payload. */
  void push(AnyValue<?> event);

  /** Notifies the consumer that its push connection has been disconnected. */
  void disconnectPushConsumer();
}
