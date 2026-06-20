package io.github.mundanej.mjo.transaction;

import java.util.Objects;

/** Opaque local handle for a transaction coordinator entry. */
public final class TransactionHandle {

  private final TransactionId transactionId;
  private final long generation;

  TransactionHandle(TransactionId transactionId, long generation) {
    this.transactionId = Objects.requireNonNull(transactionId, "transactionId");
    if (generation < 1) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.STALE_TRANSACTION,
          "transaction handle generation must be positive");
    }
    this.generation = generation;
  }

  /** Returns the local transaction ID attached to this handle. */
  public TransactionId transactionId() {
    return transactionId;
  }

  long generation() {
    return generation;
  }
}
