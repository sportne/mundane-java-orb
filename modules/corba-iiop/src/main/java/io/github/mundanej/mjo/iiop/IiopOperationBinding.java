package io.github.mundanej.mjo.iiop;

import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.Objects;

/** Network-dispatch binding for one IDL operation and its request/reply codec. */
public record IiopOperationBinding(IdlOperationDescriptor operation, IiopInvocationCodec codec) {

  /** Creates a validated operation binding. */
  public IiopOperationBinding {
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(codec, "codec");
  }
}
