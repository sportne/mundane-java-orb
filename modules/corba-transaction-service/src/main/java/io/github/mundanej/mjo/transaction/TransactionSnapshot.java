package io.github.mundanej.mjo.transaction;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Immutable snapshot of one local transaction coordinator entry. */
public record TransactionSnapshot(
    TransactionId transactionId,
    Instant beganAt,
    Instant expiresAt,
    List<TransactionResourceSnapshot> resources) {

  /** Creates a transaction snapshot without timeout metadata for simple model tests. */
  public TransactionSnapshot(
      TransactionId transactionId, List<TransactionResourceSnapshot> resources) {
    this(
        transactionId,
        Instant.EPOCH,
        Instant.EPOCH.plus(TransactionTimeoutPolicy.DEFAULT_TIMEOUT),
        resources);
  }

  /** Creates a transaction snapshot with an immutable resource list. */
  public TransactionSnapshot {
    Objects.requireNonNull(transactionId, "transactionId");
    Objects.requireNonNull(beganAt, "beganAt");
    Objects.requireNonNull(expiresAt, "expiresAt");
    if (resources == null) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.MALFORMED_IDENTIFIER,
          "transaction resources must not be null");
    }
    resources = List.copyOf(resources);
  }
}
