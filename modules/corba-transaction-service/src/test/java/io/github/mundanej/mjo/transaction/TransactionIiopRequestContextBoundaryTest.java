package io.github.mundanej.mjo.transaction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class TransactionIiopRequestContextBoundaryTest {

  @Test
  void encodesAndDecodesDescriptorBackedRequestContext() {
    Instant now = Instant.parse("2026-06-20T12:00:00Z");
    LocalTransactionCoordinator coordinator = coordinatorAt(now);
    TransactionHandle handle = coordinator.begin("txn-1");
    TransactionIiopRequestContextBoundary boundary =
        new TransactionIiopRequestContextBoundary(coordinator);
    TransactionIiopRequestContextCodec codec = new TransactionIiopRequestContextCodec();

    TransactionIiopRequestContextDescriptor descriptor = boundary.exportRequestContext(handle);
    TransactionPropagationContext decoded = codec.decode(descriptor);
    TransactionSnapshot snapshot = boundary.validateRequestContext(descriptor);

    assertEquals(
        TransactionIiopRequestContextDescriptor.TRANSACTION_SERVICE_CONTEXT_ID,
        descriptor.contextId());
    assertEquals(
        "mjo-txn-prop-v1|dHhuLTE|1|2026-06-20T12:00:00Z|2026-06-20T12:01:00Z",
        new String(descriptor.contextData(), StandardCharsets.UTF_8));
    assertEquals(coordinator.exportPropagationContext(handle), decoded);
    assertEquals(TransactionState.ACTIVE, snapshot.state());
  }

  @Test
  void descriptorCopiesRequestContextBytesAndHasDeterministicIdentity() {
    byte[] bytes = "context".getBytes(StandardCharsets.UTF_8);
    TransactionIiopRequestContextDescriptor descriptor =
        new TransactionIiopRequestContextDescriptor(
            TransactionIiopRequestContextDescriptor.TRANSACTION_SERVICE_CONTEXT_ID, bytes);
    TransactionIiopRequestContextDescriptor same =
        new TransactionIiopRequestContextDescriptor(
            TransactionIiopRequestContextDescriptor.TRANSACTION_SERVICE_CONTEXT_ID,
            "context".getBytes(StandardCharsets.UTF_8));

    bytes[0] = 'x';
    byte[] exported = descriptor.contextData();
    exported[0] = 'y';

    assertArrayEquals("context".getBytes(StandardCharsets.UTF_8), descriptor.contextData());
    assertEquals(descriptor, same);
    assertEquals(descriptor.hashCode(), same.hashCode());
    assertNotEquals(
        descriptor,
        new TransactionIiopRequestContextDescriptor(
            42, "context".getBytes(StandardCharsets.UTF_8)));
    assertEquals(
        "TransactionIiopRequestContextDescriptor[contextId=1296716888, contextDataLength=7]",
        descriptor.toString());
  }

  @Test
  void validatesLoopbackRequestContextListAndIgnoresUnrelatedContexts() {
    Instant now = Instant.parse("2026-06-20T12:00:00Z");
    LocalTransactionCoordinator coordinator = coordinatorAt(now);
    TransactionHandle handle = coordinator.begin("txn-1");
    TransactionIiopRequestContextBoundary boundary =
        new TransactionIiopRequestContextBoundary(coordinator);
    TransactionIiopRequestContextDescriptor unrelated =
        new TransactionIiopRequestContextDescriptor(99, new byte[] {1});
    TransactionIiopRequestContextDescriptor transactionContext =
        boundary.exportRequestContext(handle);

    Optional<TransactionSnapshot> snapshot =
        boundary.validateRequestContexts(List.of(unrelated, transactionContext));
    Optional<TransactionSnapshot> absent = boundary.validateRequestContexts(List.of(unrelated));

    assertTrue(snapshot.isPresent());
    assertEquals(TransactionState.ACTIVE, snapshot.orElseThrow().state());
    assertTrue(absent.isEmpty());
  }

  @Test
  void rejectsMalformedDuplicateAndWrongRequestContexts() {
    TransactionIiopRequestContextCodec codec = new TransactionIiopRequestContextCodec();
    TransactionIiopRequestContextDescriptor malformedUtf8 =
        new TransactionIiopRequestContextDescriptor(
            TransactionIiopRequestContextDescriptor.TRANSACTION_SERVICE_CONTEXT_ID,
            new byte[] {(byte) 0xC3, 0x28});
    TransactionIiopRequestContextDescriptor wrongId =
        new TransactionIiopRequestContextDescriptor(7, "context".getBytes(StandardCharsets.UTF_8));
    TransactionIiopRequestContextDescriptor transactionContext =
        new TransactionIiopRequestContextDescriptor(
            TransactionIiopRequestContextDescriptor.TRANSACTION_SERVICE_CONTEXT_ID,
            "mjo-txn-prop-v1|dHhuLTE|1|2026-06-20T12:00:00Z|2026-06-20T12:01:00Z"
                .getBytes(StandardCharsets.UTF_8));

    TransactionServiceException malformed =
        assertThrows(TransactionServiceException.class, () -> codec.decode(malformedUtf8));
    TransactionServiceException wrong =
        assertThrows(TransactionServiceException.class, () -> codec.decode(wrongId));
    TransactionServiceException duplicate =
        assertThrows(
            TransactionServiceException.class,
            () -> codec.findTransactionContext(List.of(transactionContext, transactionContext)));
    TransactionServiceException empty =
        assertThrows(
            TransactionServiceException.class,
            () ->
                new TransactionIiopRequestContextDescriptor(
                    TransactionIiopRequestContextDescriptor.TRANSACTION_SERVICE_CONTEXT_ID,
                    new byte[0]));
    TransactionServiceException oversized =
        assertThrows(
            TransactionServiceException.class,
            () ->
                new TransactionIiopRequestContextDescriptor(
                    TransactionIiopRequestContextDescriptor.TRANSACTION_SERVICE_CONTEXT_ID,
                    new byte[TransactionPropagationCodec.MAX_ENCODED_LENGTH + 1]));
    TransactionServiceException tooMany =
        assertThrows(
            TransactionServiceException.class,
            () ->
                codec.findTransactionContext(
                    Collections.nCopies(
                        TransactionIiopRequestContextCodec.MAX_REQUEST_CONTEXTS + 1, wrongId)));

    assertEquals(TransactionServiceDiagnosticCodes.MALFORMED_REQUEST_CONTEXT, malformed.code());
    assertEquals(TransactionServiceDiagnosticCodes.MALFORMED_REQUEST_CONTEXT, wrong.code());
    assertEquals(TransactionServiceDiagnosticCodes.MALFORMED_REQUEST_CONTEXT, duplicate.code());
    assertEquals(TransactionServiceDiagnosticCodes.MALFORMED_REQUEST_CONTEXT, empty.code());
    assertEquals(TransactionServiceDiagnosticCodes.MALFORMED_REQUEST_CONTEXT, oversized.code());
    assertEquals(TransactionServiceDiagnosticCodes.MALFORMED_REQUEST_CONTEXT, tooMany.code());
  }

  @Test
  void rejectsUnknownAndStaleRequestContextsThroughCoordinatorValidation() {
    Instant now = Instant.parse("2026-06-20T12:00:00Z");
    LocalTransactionCoordinator coordinator = coordinatorAt(now);
    TransactionHandle handle = coordinator.begin("txn-1");
    TransactionIiopRequestContextBoundary boundary =
        new TransactionIiopRequestContextBoundary(coordinator);
    TransactionIiopRequestContextDescriptor descriptor = boundary.exportRequestContext(handle);
    coordinator.forget("txn-1");

    TransactionServiceException unknown =
        assertThrows(
            TransactionServiceException.class, () -> boundary.validateRequestContext(descriptor));
    coordinator.begin("txn-1");
    TransactionServiceException stale =
        assertThrows(
            TransactionServiceException.class, () -> boundary.validateRequestContext(descriptor));

    assertEquals(TransactionServiceDiagnosticCodes.TRANSACTION_NOT_FOUND, unknown.code());
    assertEquals(TransactionServiceDiagnosticCodes.STALE_PROPAGATION_CONTEXT, stale.code());
  }

  @Test
  void rejectsExpiredRequestContextThroughCoordinatorValidation() {
    Instant now = Instant.parse("2026-06-20T12:00:00Z");
    MutableClock clock = new MutableClock(now);
    LocalTransactionCoordinator coordinator =
        new LocalTransactionCoordinator(
            TransactionServiceOptions.defaults(),
            new TransactionTimeoutPolicy(Duration.ofSeconds(5), Duration.ofMinutes(1)),
            clock);
    TransactionHandle handle = coordinator.begin("txn-1");
    TransactionIiopRequestContextBoundary boundary =
        new TransactionIiopRequestContextBoundary(coordinator);
    TransactionIiopRequestContextDescriptor descriptor = boundary.exportRequestContext(handle);
    clock.now = now.plusSeconds(5);

    TransactionServiceException expired =
        assertThrows(
            TransactionServiceException.class, () -> boundary.validateRequestContext(descriptor));

    assertEquals(TransactionServiceDiagnosticCodes.PROPAGATION_CONTEXT_EXPIRED, expired.code());
    assertEquals(TransactionState.TIMEOUT_ROLLED_BACK, coordinator.snapshot(handle).state());
  }

  @Test
  void rejectsBoundaryUseAfterCleanShutdown() {
    LocalTransactionCoordinator coordinator = new LocalTransactionCoordinator();
    TransactionHandle handle = coordinator.begin("txn-1");
    TransactionIiopRequestContextBoundary boundary =
        new TransactionIiopRequestContextBoundary(coordinator);
    TransactionIiopRequestContextDescriptor descriptor = boundary.exportRequestContext(handle);

    boundary.close();
    boundary.close();

    TransactionServiceException exportAfterClose =
        assertThrows(
            TransactionServiceException.class, () -> boundary.exportRequestContext(handle));
    TransactionServiceException validateAfterClose =
        assertThrows(
            TransactionServiceException.class, () -> boundary.validateRequestContext(descriptor));

    assertTrue(boundary.isClosed());
    assertEquals(
        TransactionServiceDiagnosticCodes.REQUEST_CONTEXT_BOUNDARY_CLOSED, exportAfterClose.code());
    assertEquals(
        TransactionServiceDiagnosticCodes.REQUEST_CONTEXT_BOUNDARY_CLOSED,
        validateAfterClose.code());
  }

  @Test
  void requestContextTypesAvoidForbiddenRuntimeMechanisms() {
    assertFalse(Serializable.class.isAssignableFrom(TransactionIiopRequestContextBoundary.class));
    assertFalse(Serializable.class.isAssignableFrom(TransactionIiopRequestContextCodec.class));
    assertFalse(Serializable.class.isAssignableFrom(TransactionIiopRequestContextDescriptor.class));
    assertTrue(Modifier.isFinal(TransactionIiopRequestContextBoundary.class.getModifiers()));
    assertTrue(Modifier.isFinal(TransactionIiopRequestContextCodec.class.getModifiers()));
    assertTrue(Modifier.isFinal(TransactionIiopRequestContextDescriptor.class.getModifiers()));
  }

  private static LocalTransactionCoordinator coordinatorAt(Instant now) {
    return new LocalTransactionCoordinator(
        TransactionServiceOptions.defaults(),
        TransactionTimeoutPolicy.defaults(),
        Clock.fixed(now, ZoneOffset.UTC));
  }

  private static final class MutableClock extends Clock {
    private Instant now;

    private MutableClock(Instant now) {
      this.now = now;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(now, zone);
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
