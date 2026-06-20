package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.transaction.LocalTransactionCoordinator;
import io.github.mundanej.mjo.transaction.TransactionHandle;
import io.github.mundanej.mjo.transaction.TransactionIiopRequestContextBoundary;
import io.github.mundanej.mjo.transaction.TransactionIiopRequestContextDescriptor;
import io.github.mundanej.mjo.transaction.TransactionRecoveryBoundary;
import io.github.mundanej.mjo.transaction.TransactionResourceParticipant;
import io.github.mundanej.mjo.transaction.TransactionResourceVote;
import io.github.mundanej.mjo.transaction.TransactionServiceDiagnosticCodes;
import io.github.mundanej.mjo.transaction.TransactionServiceException;
import io.github.mundanej.mjo.transaction.TransactionState;
import io.github.mundanej.mjo.transaction.TransactionTimeoutPolicy;
import java.time.Duration;

/** Native Image smoke coverage for the supported local Transaction Service slice. */
public final class TransactionServiceNativeSmoke {

  private TransactionServiceNativeSmoke() {}

  /** Runs the Transaction Service Native Image smoke checks. */
  public static void main(String[] args) {
    LocalTransactionCoordinator coordinator = new LocalTransactionCoordinator();
    TransactionHandle committed = coordinator.begin("txn-commit");
    RecordingResource commitResource = new RecordingResource(TransactionResourceVote.COMMIT);
    coordinator.enlist(committed, "resource-commit", commitResource);
    SmokeAssertions.requireEquals(
        TransactionState.COMMITTED, coordinator.commit(committed).state(), "local commit");
    SmokeAssertions.require(commitResource.prepared, "commit path prepared resource");
    SmokeAssertions.require(commitResource.committed, "commit path committed resource");

    TransactionHandle rolledBack = coordinator.begin("txn-rollback");
    RecordingResource rollbackResource = new RecordingResource(TransactionResourceVote.COMMIT);
    coordinator.enlist(rolledBack, "resource-rollback", rollbackResource);
    SmokeAssertions.requireEquals(
        TransactionState.ROLLED_BACK, coordinator.rollback(rolledBack).state(), "local rollback");
    SmokeAssertions.require(rollbackResource.rolledBack, "rollback path rolled back resource");

    assertDiagnostic(
        TransactionServiceDiagnosticCodes.INVALID_TIMEOUT,
        () -> new TransactionTimeoutPolicy(Duration.ZERO, Duration.ofSeconds(1)),
        "hostile timeout policy");
    assertDiagnostic(
        TransactionServiceDiagnosticCodes.INVALID_TIMEOUT,
        () ->
            new LocalTransactionCoordinator()
                .begin("txn-timeout", TransactionTimeoutPolicy.DEFAULT_MAX_TIMEOUT.plusSeconds(1)),
        "hostile requested timeout");

    TransactionHandle propagated = coordinator.begin("txn-propagated");
    TransactionIiopRequestContextBoundary boundary =
        new TransactionIiopRequestContextBoundary(coordinator);
    TransactionIiopRequestContextDescriptor requestContext =
        boundary.exportRequestContext(propagated);
    SmokeAssertions.requireEquals(
        TransactionState.ACTIVE,
        boundary.validateRequestContext(requestContext).state(),
        "request context validation");
    assertDiagnostic(
        TransactionServiceDiagnosticCodes.MALFORMED_REQUEST_CONTEXT,
        () ->
            boundary.validateRequestContext(
                new TransactionIiopRequestContextDescriptor(7, new byte[] {1})),
        "wrong request context");
    boundary.close();
    assertDiagnostic(
        TransactionServiceDiagnosticCodes.REQUEST_CONTEXT_BOUNDARY_CLOSED,
        () -> boundary.exportRequestContext(propagated),
        "request context clean shutdown");

    TransactionRecoveryBoundary recovery = new TransactionRecoveryBoundary();
    SmokeAssertions.require(!recovery.policy().durableRecoveryEnabled(), "recovery disabled");
    assertDiagnostic(
        TransactionServiceDiagnosticCodes.DURABLE_RECOVERY_DISABLED,
        recovery::recoverDurableTransactions,
        "durable recovery disabled");
  }

  private static void assertDiagnostic(
      DiagnosticCode code, SmokeAssertions.ThrowingAction action, String label) {
    try {
      action.run();
    } catch (TransactionServiceException expected) {
      SmokeAssertions.requireEquals(code, expected.code(), label);
      return;
    } catch (Exception exception) {
      throw new AssertionError("Native Image smoke failed: " + label, exception);
    }
    throw new AssertionError("Native Image smoke failed: " + label + "; expected diagnostic");
  }

  private static final class RecordingResource implements TransactionResourceParticipant {
    private final TransactionResourceVote vote;
    private boolean prepared;
    private boolean committed;
    private boolean rolledBack;

    private RecordingResource(TransactionResourceVote vote) {
      this.vote = vote;
    }

    @Override
    public TransactionResourceVote prepare() {
      prepared = true;
      return vote;
    }

    @Override
    public void commit() {
      committed = true;
    }

    @Override
    public void rollback() {
      rolledBack = true;
    }
  }
}
