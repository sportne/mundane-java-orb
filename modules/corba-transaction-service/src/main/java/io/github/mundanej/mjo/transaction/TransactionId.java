package io.github.mundanej.mjo.transaction;

/** Stable local identifier for a transaction owned by one coordinator. */
public record TransactionId(String value) {

  /** Creates a validated transaction identifier. */
  public TransactionId {
    value =
        TransactionNames.requireIdentifier(
            value, "transaction ID", TransactionServiceOptions.modelLimits());
  }
}
