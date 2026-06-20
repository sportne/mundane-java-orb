package io.github.mundanej.mjo.transaction;

import java.util.Objects;

/** Local recovery boundary that documents durable recovery as explicitly disabled. */
public final class TransactionRecoveryBoundary {

  private final TransactionRecoveryPolicy policy;

  /** Creates a boundary with disabled durable recovery. */
  public TransactionRecoveryBoundary() {
    this(TransactionRecoveryPolicy.disabled());
  }

  /** Creates a boundary with a caller-provided recovery policy. */
  public TransactionRecoveryBoundary(TransactionRecoveryPolicy policy) {
    this.policy = Objects.requireNonNull(policy, "policy");
  }

  /** Returns the active recovery policy. */
  public TransactionRecoveryPolicy policy() {
    return policy;
  }

  /** Rejects durable recovery log creation for this local-only subset. */
  public void requireDurableRecoverySupported() {
    if (!policy.durableRecoveryEnabled()) {
      throw durableRecoveryDisabled();
    }
  }

  /** Rejects durable recovery replay for this local-only subset. */
  public void recoverDurableTransactions() {
    if (!policy.durableRecoveryEnabled()) {
      throw durableRecoveryDisabled();
    }
  }

  /** Returns whether a terminal state needs durable replay in this subset. */
  public boolean requiresDurableReplay(TransactionSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    return false;
  }

  private static TransactionServiceException durableRecoveryDisabled() {
    return new TransactionServiceException(
        TransactionServiceDiagnosticCodes.DURABLE_RECOVERY_DISABLED,
        "durable transaction recovery is disabled for the local subset");
  }
}
