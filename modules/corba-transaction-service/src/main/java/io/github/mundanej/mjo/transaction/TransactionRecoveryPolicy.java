package io.github.mundanej.mjo.transaction;

/** Explicit durable-recovery policy for the local Transaction Service subset. */
public record TransactionRecoveryPolicy(boolean durableRecoveryEnabled) {

  /** Creates a policy and rejects unapproved durable recovery. */
  public TransactionRecoveryPolicy {
    if (durableRecoveryEnabled) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.DURABLE_RECOVERY_DISABLED,
          "durable transaction recovery is disabled for the local subset");
    }
  }

  /** Returns the default policy: durable recovery disabled. */
  public static TransactionRecoveryPolicy disabled() {
    return new TransactionRecoveryPolicy(false);
  }
}
