package io.github.mundanej.mjo.transaction;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Loopback IIOP request-context boundary for local Transaction Service propagation metadata. */
public final class TransactionIiopRequestContextBoundary implements AutoCloseable {

  private final LocalTransactionCoordinator coordinator;
  private final TransactionIiopRequestContextCodec codec;
  private boolean closed;

  /** Creates a boundary around a local coordinator with the default request-context codec. */
  public TransactionIiopRequestContextBoundary(LocalTransactionCoordinator coordinator) {
    this(coordinator, new TransactionIiopRequestContextCodec());
  }

  /** Creates a boundary around a local coordinator with caller-provided request-context codec. */
  public TransactionIiopRequestContextBoundary(
      LocalTransactionCoordinator coordinator, TransactionIiopRequestContextCodec codec) {
    this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
    this.codec = Objects.requireNonNull(codec, "codec");
  }

  /** Exports local propagation metadata for a current transaction handle. */
  public synchronized TransactionIiopRequestContextDescriptor exportRequestContext(
      TransactionHandle handle) {
    requireOpen();
    return codec.encode(coordinator.exportPropagationContext(handle));
  }

  /** Decodes and validates a single transaction request context against the local coordinator. */
  public synchronized TransactionSnapshot validateRequestContext(
      TransactionIiopRequestContextDescriptor descriptor) {
    requireOpen();
    return coordinator.validatePropagationContext(codec.decode(descriptor));
  }

  /** Decodes and validates the transaction request context when one is present in the list. */
  public synchronized Optional<TransactionSnapshot> validateRequestContexts(
      List<TransactionIiopRequestContextDescriptor> descriptors) {
    requireOpen();
    return codec.findTransactionContext(descriptors).map(this::validateRequestContext);
  }

  /** Returns whether this request-context boundary is closed. */
  public synchronized boolean isClosed() {
    return closed;
  }

  /** Closes this local boundary without starting or stopping peer execution. */
  @Override
  public synchronized void close() {
    closed = true;
  }

  private void requireOpen() {
    if (closed) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.REQUEST_CONTEXT_BOUNDARY_CLOSED,
          "Transaction Service request-context boundary is closed");
    }
  }
}
