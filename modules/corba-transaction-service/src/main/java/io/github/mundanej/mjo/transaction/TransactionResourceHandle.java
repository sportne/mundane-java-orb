package io.github.mundanej.mjo.transaction;

import java.util.Objects;

/** Opaque local handle for a resource enlistment owned by one transaction. */
public final class TransactionResourceHandle {

  private final TransactionId transactionId;
  private final long transactionGeneration;
  private final TransactionResourceId resourceId;
  private final long resourceGeneration;

  TransactionResourceHandle(
      TransactionId transactionId,
      long transactionGeneration,
      TransactionResourceId resourceId,
      long resourceGeneration) {
    this.transactionId = Objects.requireNonNull(transactionId, "transactionId");
    this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
    if (transactionGeneration < 1) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.STALE_TRANSACTION,
          "transaction handle generation must be positive");
    }
    if (resourceGeneration < 1) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.STALE_RESOURCE,
          "resource handle generation must be positive");
    }
    this.transactionGeneration = transactionGeneration;
    this.resourceGeneration = resourceGeneration;
  }

  /** Returns the local transaction ID attached to this resource handle. */
  public TransactionId transactionId() {
    return transactionId;
  }

  /** Returns the local resource ID attached to this handle. */
  public TransactionResourceId resourceId() {
    return resourceId;
  }

  long transactionGeneration() {
    return transactionGeneration;
  }

  long resourceGeneration() {
    return resourceGeneration;
  }
}
