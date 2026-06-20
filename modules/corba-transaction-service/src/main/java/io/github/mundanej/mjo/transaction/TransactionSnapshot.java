package io.github.mundanej.mjo.transaction;

import java.util.List;
import java.util.Objects;

/** Immutable snapshot of one local transaction coordinator entry. */
public record TransactionSnapshot(
    TransactionId transactionId, List<TransactionResourceSnapshot> resources) {

  /** Creates a transaction snapshot with an immutable resource list. */
  public TransactionSnapshot {
    Objects.requireNonNull(transactionId, "transactionId");
    if (resources == null) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.MALFORMED_IDENTIFIER,
          "transaction resources must not be null");
    }
    resources = List.copyOf(resources);
  }
}
