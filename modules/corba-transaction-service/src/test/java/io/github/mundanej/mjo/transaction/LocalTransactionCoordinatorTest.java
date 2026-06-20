package io.github.mundanej.mjo.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class LocalTransactionCoordinatorTest {

  @Test
  void createsAndLooksUpTransactions() {
    LocalTransactionCoordinator coordinator = new LocalTransactionCoordinator();

    TransactionHandle handle = coordinator.begin("txn-1");

    assertEquals("txn-1", handle.transactionId().value());
    assertEquals(
        List.of(new TransactionSnapshot(handle.transactionId(), List.of())), coordinator.list());
    assertTrue(coordinator.lookup("txn-1").isPresent());
  }

  @Test
  void rejectsDuplicateTransactionsAndMissingForget() {
    LocalTransactionCoordinator coordinator = new LocalTransactionCoordinator();
    coordinator.begin("txn-1");

    TransactionServiceException duplicate =
        assertThrows(TransactionServiceException.class, () -> coordinator.begin("txn-1"));
    TransactionServiceException missing =
        assertThrows(TransactionServiceException.class, () -> coordinator.forget("missing"));

    assertEquals(TransactionServiceDiagnosticCodes.TRANSACTION_ALREADY_EXISTS, duplicate.code());
    assertEquals(TransactionServiceDiagnosticCodes.TRANSACTION_NOT_FOUND, missing.code());
  }

  @Test
  void enforcesConfiguredTransactionAndResourceLimits() {
    LocalTransactionCoordinator coordinator =
        new LocalTransactionCoordinator(new TransactionServiceOptions(1, 1, 16));
    TransactionHandle handle = coordinator.begin("txn-1");
    coordinator.enlist(handle, "resource-1");

    TransactionServiceException transactionLimit =
        assertThrows(TransactionServiceException.class, () -> coordinator.begin("txn-2"));
    TransactionServiceException resourceLimit =
        assertThrows(
            TransactionServiceException.class, () -> coordinator.enlist(handle, "resource-2"));

    assertEquals(
        TransactionServiceDiagnosticCodes.TRANSACTION_LIMIT_EXCEEDED, transactionLimit.code());
    assertEquals(TransactionServiceDiagnosticCodes.RESOURCE_LIMIT_EXCEEDED, resourceLimit.code());
  }

  @Test
  void supportsCallerConfiguredLimitsAboveDefaults() {
    LocalTransactionCoordinator coordinator =
        new LocalTransactionCoordinator(new TransactionServiceOptions(2, 65, 130));
    String transactionId = "T".repeat(130);
    TransactionHandle handle = coordinator.begin(transactionId);

    for (int index = 0; index < 65; index++) {
      coordinator.enlist(handle, "resource-" + index);
    }

    assertEquals(transactionId, handle.transactionId().value());
    assertEquals(65, coordinator.snapshot(handle).resources().size());
  }

  @Test
  void enlistsAndDelistsResourcesInDeterministicOrder() {
    LocalTransactionCoordinator coordinator = new LocalTransactionCoordinator();
    TransactionHandle handle = coordinator.begin("txn-1");

    TransactionResourceHandle first = coordinator.enlist(handle, "resource-a");
    coordinator.enlist(handle, "resource-b");
    TransactionResourceSnapshot delisted = coordinator.delist(first);

    assertEquals("resource-a", delisted.resourceId().value());
    assertEquals(
        List.of(new TransactionResourceSnapshot(new TransactionResourceId("resource-b"))),
        coordinator.snapshot(handle).resources());
  }

  @Test
  void rejectsDuplicateMissingAndMalformedResourceIdentifiers() {
    LocalTransactionCoordinator coordinator = new LocalTransactionCoordinator();
    TransactionHandle handle = coordinator.begin("txn-1");
    coordinator.enlist(handle, "resource-1");

    TransactionServiceException duplicate =
        assertThrows(
            TransactionServiceException.class, () -> coordinator.enlist(handle, "resource-1"));
    TransactionServiceException missing =
        assertThrows(
            TransactionServiceException.class,
            () -> coordinator.delist("txn-1", "missing-resource"));
    TransactionServiceException malformed =
        assertThrows(TransactionServiceException.class, () -> coordinator.enlist(handle, " "));

    assertEquals(TransactionServiceDiagnosticCodes.RESOURCE_ALREADY_ENLISTED, duplicate.code());
    assertEquals(TransactionServiceDiagnosticCodes.RESOURCE_NOT_FOUND, missing.code());
    assertEquals(TransactionServiceDiagnosticCodes.MALFORMED_IDENTIFIER, malformed.code());
  }

  @Test
  void rejectsInvalidLimitsAndMalformedTransactionIdentifiers() {
    TransactionServiceException invalidLimit =
        assertThrows(
            TransactionServiceException.class, () -> new TransactionServiceOptions(0, 1, 1));
    TransactionServiceException blankTransaction =
        assertThrows(
            TransactionServiceException.class, () -> new LocalTransactionCoordinator().begin(""));
    TransactionServiceException longTransaction =
        assertThrows(
            TransactionServiceException.class,
            () ->
                new LocalTransactionCoordinator(new TransactionServiceOptions(1, 1, 4))
                    .begin("too-long"));

    assertEquals(TransactionServiceDiagnosticCodes.INVALID_LIMIT, invalidLimit.code());
    assertEquals(TransactionServiceDiagnosticCodes.MALFORMED_IDENTIFIER, blankTransaction.code());
    assertEquals(TransactionServiceDiagnosticCodes.MALFORMED_IDENTIFIER, longTransaction.code());
  }

  @Test
  void detectsStaleTransactionAndResourceHandles() {
    LocalTransactionCoordinator coordinator = new LocalTransactionCoordinator();
    TransactionHandle handle = coordinator.begin("txn-1");
    TransactionResourceHandle resource = coordinator.enlist(handle, "resource-1");
    coordinator.delist(resource);

    TransactionServiceException staleResource =
        assertThrows(TransactionServiceException.class, () -> coordinator.delist(resource));
    coordinator.forget("txn-1");
    TransactionHandle replacement = coordinator.begin("txn-1");
    TransactionServiceException staleTransaction =
        assertThrows(TransactionServiceException.class, () -> coordinator.snapshot(handle));

    assertEquals(TransactionServiceDiagnosticCodes.STALE_RESOURCE, staleResource.code());
    assertEquals(TransactionServiceDiagnosticCodes.STALE_TRANSACTION, staleTransaction.code());
    assertEquals("txn-1", replacement.transactionId().value());
  }

  @Test
  void snapshotsAreImmutableAndIndependent() {
    LocalTransactionCoordinator coordinator = new LocalTransactionCoordinator();
    TransactionHandle handle = coordinator.begin("txn-1");
    coordinator.enlist(handle, "resource-1");
    TransactionSnapshot snapshot = coordinator.snapshot(handle);
    coordinator.enlist(handle, "resource-2");

    assertEquals(1, snapshot.resources().size());
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            snapshot
                .resources()
                .add(new TransactionResourceSnapshot(new TransactionResourceId("other"))));
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            coordinator.list().add(new TransactionSnapshot(new TransactionId("other"), List.of())));
  }

  @Test
  void lookupMissingTransactionIsOptionalEmptyButValidatesNames() {
    LocalTransactionCoordinator coordinator = new LocalTransactionCoordinator();

    assertFalse(coordinator.lookup("missing").isPresent());
    TransactionServiceException malformed =
        assertThrows(TransactionServiceException.class, () -> coordinator.lookup(" "));

    assertEquals(TransactionServiceDiagnosticCodes.MALFORMED_IDENTIFIER, malformed.code());
  }
}
