package io.github.mundanej.mjo.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.Base64;
import org.junit.jupiter.api.Test;

final class TransactionPropagationContextTest {

  @Test
  void exportsAndValidatesImmutableLocalPropagationContext() {
    Instant now = Instant.parse("2026-06-20T12:00:00Z");
    LocalTransactionCoordinator coordinator = coordinatorAt(now);
    TransactionHandle handle = coordinator.begin("txn-1");

    TransactionPropagationContext context = coordinator.exportPropagationContext(handle);
    TransactionSnapshot snapshot = coordinator.validatePropagationContext(context);

    assertEquals("txn-1", context.transactionId().value());
    assertEquals(now, context.beganAt());
    assertEquals(now.plus(TransactionTimeoutPolicy.DEFAULT_TIMEOUT), context.expiresAt());
    assertEquals(TransactionState.ACTIVE, snapshot.state());
  }

  @Test
  void encodesAndDecodesDeterministicBoundedText() {
    Instant now = Instant.parse("2026-06-20T12:00:00Z");
    LocalTransactionCoordinator coordinator = coordinatorAt(now);
    TransactionHandle handle = coordinator.begin("txn-1");
    TransactionPropagationCodec codec = new TransactionPropagationCodec();

    String encoded = codec.encode(coordinator.exportPropagationContext(handle));
    TransactionPropagationContext decoded = codec.decode(encoded);

    assertEquals("mjo-txn-prop-v1|dHhuLTE|1|2026-06-20T12:00:00Z|2026-06-20T12:01:00Z", encoded);
    assertEquals(coordinator.exportPropagationContext(handle), decoded);
    assertEquals(
        TransactionState.ACTIVE, coordinator.validatePropagationContext(encoded, codec).state());
  }

  @Test
  void rejectsMalformedOversizedAndExcessFieldContexts() {
    TransactionPropagationCodec codec = new TransactionPropagationCodec();
    String oversized = "x".repeat(TransactionPropagationCodec.MAX_ENCODED_LENGTH + 1);

    TransactionServiceException blank =
        assertThrows(TransactionServiceException.class, () -> codec.decode(" "));
    TransactionServiceException unsupported =
        assertThrows(
            TransactionServiceException.class,
            () -> codec.decode("wrong|dHhu|1|2026-06-20T12:00:00Z|2026-06-20T12:01:00Z"));
    TransactionServiceException fieldCount =
        assertThrows(
            TransactionServiceException.class,
            () ->
                codec.decode("mjo-txn-prop-v1|dHhu|1|2026-06-20T12:00:00Z|2026-06-20T12:01:00Z|x"));
    TransactionServiceException badGeneration =
        assertThrows(
            TransactionServiceException.class,
            () ->
                codec.decode(
                    "mjo-txn-prop-v1|dHhu|not-a-number|2026-06-20T12:00:00Z|2026-06-20T12:01:00Z"));
    TransactionServiceException tooLarge =
        assertThrows(TransactionServiceException.class, () -> codec.decode(oversized));
    TransactionServiceException blankDecodedId =
        assertThrows(
            TransactionServiceException.class,
            () -> codec.decode("mjo-txn-prop-v1||1|2026-06-20T12:00:00Z|2026-06-20T12:01:00Z"));
    TransactionServiceException oversizedDecodedId =
        assertThrows(
            TransactionServiceException.class,
            () ->
                codec.decode(
                    "mjo-txn-prop-v1|"
                        + encodedIdentifier(
                            "x".repeat(TransactionServiceOptions.MAX_SUPPORTED_LIMIT + 1))
                        + "|1|2026-06-20T12:00:00Z|2026-06-20T12:01:00Z"));

    assertEquals(TransactionServiceDiagnosticCodes.MALFORMED_PROPAGATION_CONTEXT, blank.code());
    assertEquals(
        TransactionServiceDiagnosticCodes.MALFORMED_PROPAGATION_CONTEXT, unsupported.code());
    assertEquals(
        TransactionServiceDiagnosticCodes.MALFORMED_PROPAGATION_CONTEXT, fieldCount.code());
    assertEquals(
        TransactionServiceDiagnosticCodes.MALFORMED_PROPAGATION_CONTEXT, badGeneration.code());
    assertEquals(TransactionServiceDiagnosticCodes.MALFORMED_PROPAGATION_CONTEXT, tooLarge.code());
    assertEquals(
        TransactionServiceDiagnosticCodes.MALFORMED_PROPAGATION_CONTEXT, blankDecodedId.code());
    assertEquals(
        TransactionServiceDiagnosticCodes.MALFORMED_PROPAGATION_CONTEXT, oversizedDecodedId.code());
  }

  @Test
  void rejectsUnknownAndStalePropagationContexts() {
    Instant now = Instant.parse("2026-06-20T12:00:00Z");
    LocalTransactionCoordinator coordinator = coordinatorAt(now);
    TransactionHandle handle = coordinator.begin("txn-1");
    TransactionPropagationContext context = coordinator.exportPropagationContext(handle);
    coordinator.forget("txn-1");

    TransactionServiceException unknown =
        assertThrows(
            TransactionServiceException.class,
            () -> coordinator.validatePropagationContext(context));
    TransactionHandle replacement = coordinator.begin("txn-1");
    TransactionServiceException stale =
        assertThrows(
            TransactionServiceException.class,
            () -> coordinator.validatePropagationContext(context));

    assertEquals(TransactionServiceDiagnosticCodes.TRANSACTION_NOT_FOUND, unknown.code());
    assertEquals(TransactionServiceDiagnosticCodes.STALE_PROPAGATION_CONTEXT, stale.code());
    assertEquals("txn-1", replacement.transactionId().value());
  }

  @Test
  void rejectsExpiredPropagationContextThroughTimeoutPolicy() {
    Instant now = Instant.parse("2026-06-20T12:00:00Z");
    MutableClock clock = new MutableClock(now);
    LocalTransactionCoordinator coordinator =
        new LocalTransactionCoordinator(
            TransactionServiceOptions.defaults(),
            new TransactionTimeoutPolicy(Duration.ofSeconds(5), Duration.ofMinutes(1)),
            clock);
    TransactionHandle handle = coordinator.begin("txn-1");
    TransactionPropagationContext context = coordinator.exportPropagationContext(handle);
    clock.now = now.plusSeconds(5);

    TransactionServiceException expired =
        assertThrows(
            TransactionServiceException.class,
            () -> coordinator.validatePropagationContext(context));

    assertEquals(TransactionServiceDiagnosticCodes.PROPAGATION_CONTEXT_EXPIRED, expired.code());
    assertEquals(TransactionState.TIMEOUT_ROLLED_BACK, coordinator.snapshot(handle).state());
  }

  @Test
  void propagationTypesAvoidJavaSerializationBoundary() {
    assertFalse(Serializable.class.isAssignableFrom(TransactionPropagationContext.class));
    assertFalse(Serializable.class.isAssignableFrom(TransactionPropagationCodec.class));
    assertTrue(Modifier.isFinal(TransactionPropagationCodec.class.getModifiers()));
  }

  private static LocalTransactionCoordinator coordinatorAt(Instant now) {
    return new LocalTransactionCoordinator(
        TransactionServiceOptions.defaults(),
        TransactionTimeoutPolicy.defaults(),
        Clock.fixed(now, ZoneOffset.UTC));
  }

  private static String encodedIdentifier(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
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
