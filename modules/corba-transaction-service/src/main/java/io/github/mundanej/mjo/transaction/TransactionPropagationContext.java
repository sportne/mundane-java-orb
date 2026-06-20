package io.github.mundanej.mjo.transaction;

import java.time.Instant;
import java.util.Objects;

/** Immutable local propagation metadata for the supported Transaction Service subset. */
public record TransactionPropagationContext(
    TransactionId transactionId, long transactionGeneration, Instant beganAt, Instant expiresAt) {

  /** Creates a validated local propagation context. */
  public TransactionPropagationContext {
    Objects.requireNonNull(transactionId, "transactionId");
    Objects.requireNonNull(beganAt, "beganAt");
    Objects.requireNonNull(expiresAt, "expiresAt");
    if (transactionGeneration < 1) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.MALFORMED_PROPAGATION_CONTEXT,
          "transaction generation must be positive");
    }
    if (!beganAt.isBefore(expiresAt)) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.MALFORMED_PROPAGATION_CONTEXT,
          "propagation context deadline must be after begin time");
    }
  }
}
