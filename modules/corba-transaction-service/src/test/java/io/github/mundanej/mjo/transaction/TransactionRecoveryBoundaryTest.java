package io.github.mundanej.mjo.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class TransactionRecoveryBoundaryTest {

  @Test
  void defaultsToDisabledDurableRecoveryPolicy() {
    TransactionRecoveryBoundary boundary = new TransactionRecoveryBoundary();

    assertFalse(boundary.policy().durableRecoveryEnabled());
  }

  @Test
  void rejectsUnapprovedDurableRecoveryPolicy() {
    TransactionServiceException rejected =
        assertThrows(TransactionServiceException.class, () -> new TransactionRecoveryPolicy(true));

    assertEquals(TransactionServiceDiagnosticCodes.DURABLE_RECOVERY_DISABLED, rejected.code());
  }

  @Test
  void rejectsDurableRecoveryOperationsWithStableDiagnostics() {
    TransactionRecoveryBoundary boundary = new TransactionRecoveryBoundary();

    TransactionServiceException createLog =
        assertThrows(TransactionServiceException.class, boundary::requireDurableRecoverySupported);
    TransactionServiceException replay =
        assertThrows(TransactionServiceException.class, boundary::recoverDurableTransactions);

    assertEquals(TransactionServiceDiagnosticCodes.DURABLE_RECOVERY_DISABLED, createLog.code());
    assertEquals(TransactionServiceDiagnosticCodes.DURABLE_RECOVERY_DISABLED, replay.code());
  }

  @Test
  void terminalLocalStatesDoNotRequireDurableReplay() {
    LocalTransactionCoordinator coordinator = new LocalTransactionCoordinator();
    TransactionRecoveryBoundary boundary = new TransactionRecoveryBoundary();
    TransactionHandle committed = coordinator.begin("committed");
    TransactionHandle rolledBack = coordinator.begin("rolled-back");

    TransactionSnapshot committedSnapshot = coordinator.commit(committed);
    TransactionSnapshot rolledBackSnapshot = coordinator.rollback(rolledBack);

    assertEquals(TransactionState.COMMITTED, committedSnapshot.state());
    assertEquals(TransactionState.ROLLED_BACK, rolledBackSnapshot.state());
    assertFalse(boundary.requiresDurableReplay(committedSnapshot));
    assertFalse(boundary.requiresDurableReplay(rolledBackSnapshot));
  }
}
