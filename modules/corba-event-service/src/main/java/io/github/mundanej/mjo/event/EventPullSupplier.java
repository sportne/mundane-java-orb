package io.github.mundanej.mjo.event;

import io.github.mundanej.mjo.any.AnyValue;
import java.util.Optional;

/** Local pull supplier callback used by later delivery slices. */
public interface EventPullSupplier {

  /** Pulls one Event Service payload, blocking only according to caller policy. */
  Optional<AnyValue<?>> pull();

  /** Tries to pull one Event Service payload without waiting for new data. */
  Optional<AnyValue<?>> tryPull();

  /** Notifies the supplier that its pull connection has been disconnected. */
  void disconnectPullSupplier();
}
