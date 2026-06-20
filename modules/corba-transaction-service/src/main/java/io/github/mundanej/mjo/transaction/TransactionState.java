package io.github.mundanej.mjo.transaction;

/** Supported local transaction states for the bounded Transaction Service subset. */
public enum TransactionState {
  ACTIVE,
  ROLLBACK_ONLY,
  COMMITTED,
  ROLLED_BACK,
  TIMEOUT_ROLLED_BACK,
  HEURISTIC_MIXED
}
