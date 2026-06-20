package io.github.mundanej.mjo.transaction;

/** Stable local identifier for a resource enlisted with one transaction. */
public record TransactionResourceId(String value) {

  /** Creates a validated transaction resource identifier. */
  public TransactionResourceId {
    value =
        TransactionNames.requireIdentifier(
            value, "resource ID", TransactionServiceOptions.modelLimits());
  }
}
