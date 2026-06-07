package io.github.mundanej.mjo.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import java.util.ArrayDeque;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class LocalEventServiceTest {

  @Test
  void rejectsInvalidOptionBounds() {
    EventServiceException low =
        assertThrows(EventServiceException.class, () -> new EventServiceOptions(0, 1, 1, 1));
    EventServiceException high =
        assertThrows(EventServiceException.class, () -> new EventServiceOptions(1, 65_536, 1, 1));

    assertEquals(EventServiceDiagnosticCodes.INVALID_LIMIT, low.code());
    assertEquals(EventServiceDiagnosticCodes.INVALID_LIMIT, high.code());
  }

  @Test
  void createsAndDestroysLocalChannels() {
    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel first = service.createChannel();
      LocalEventChannel second = service.createChannel();

      assertEquals(1L, first.id());
      assertEquals(2L, second.id());
      assertEquals(2, service.activeChannelCount());
      assertFalse(first.isDestroyed());

      first.destroy();

      assertTrue(first.isDestroyed());
      assertEquals(1, service.activeChannelCount());
    }
  }

  @Test
  void rejectsDuplicateChannelDestroyAndDestroyedAdminUse() {
    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel channel = service.createChannel();
      LocalEventSupplierAdmin supplierAdmin = channel.supplierAdmin();
      channel.destroy();

      EventServiceException duplicate = assertThrows(EventServiceException.class, channel::destroy);
      EventServiceException adminUse =
          assertThrows(EventServiceException.class, supplierAdmin::obtainPushConsumerProxy);

      assertEquals(EventServiceDiagnosticCodes.CHANNEL_DESTROYED, duplicate.code());
      assertEquals(EventServiceDiagnosticCodes.CHANNEL_DESTROYED, adminUse.code());
    }
  }

  @Test
  void rejectsCreateAfterServiceShutdown() {
    LocalEventService service = LocalEventService.create();
    service.close();

    EventServiceException exception =
        assertThrows(EventServiceException.class, service::createChannel);

    assertEquals(EventServiceDiagnosticCodes.SERVICE_SHUTDOWN, exception.code());
  }

  @Test
  void enforcesConfiguredChannelLimit() {
    try (LocalEventService service =
        LocalEventService.create(new EventServiceOptions(1, 1, 1, 1))) {
      service.createChannel();

      EventServiceException exception =
          assertThrows(EventServiceException.class, service::createChannel);

      assertEquals(EventServiceDiagnosticCodes.CHANNEL_LIMIT_EXCEEDED, exception.code());
    }
  }

  @Test
  void createsAdminOwnedProxyHandles() {
    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel channel = service.createChannel();
      LocalEventSupplierAdmin supplierAdmin = channel.supplierAdmin();
      LocalEventConsumerAdmin consumerAdmin = channel.consumerAdmin();

      LocalPushConsumerProxy pushConsumer = supplierAdmin.obtainPushConsumerProxy();
      LocalPullConsumerProxy pullConsumer = supplierAdmin.obtainPullConsumerProxy();
      LocalPushSupplierProxy pushSupplier = consumerAdmin.obtainPushSupplierProxy();
      LocalPullSupplierProxy pullSupplier = consumerAdmin.obtainPullSupplierProxy();

      assertEquals(channel.id(), supplierAdmin.channelId());
      assertEquals(channel.id(), consumerAdmin.channelId());
      assertProxy(pushConsumer, channel.id(), 1L, EventProxyKind.PUSH_CONSUMER);
      assertProxy(pullConsumer, channel.id(), 2L, EventProxyKind.PULL_CONSUMER);
      assertProxy(pushSupplier, channel.id(), 3L, EventProxyKind.PUSH_SUPPLIER);
      assertProxy(pullSupplier, channel.id(), 4L, EventProxyKind.PULL_SUPPLIER);
    }
  }

  @Test
  void rejectsDuplicateProxyDestroy() {
    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel channel = service.createChannel();
      LocalPushConsumerProxy proxy = channel.supplierAdmin().obtainPushConsumerProxy();
      proxy.destroy();

      EventServiceException exception = assertThrows(EventServiceException.class, proxy::destroy);

      assertTrue(proxy.isDestroyed());
      assertEquals(EventServiceDiagnosticCodes.PROXY_DESTROYED, exception.code());
    }
  }

  @Test
  void deliversPushedEventsToConnectedPushConsumers() {
    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel channel = service.createChannel();
      LocalPushConsumerProxy supplierSide = channel.supplierAdmin().obtainPushConsumerProxy();
      LocalPushSupplierProxy consumerSide = channel.consumerAdmin().obtainPushSupplierProxy();
      RecordingPushSupplier supplier = new RecordingPushSupplier();
      RecordingPushConsumer consumer = new RecordingPushConsumer();
      AnyValue<String> event = stringEvent("alpha");

      supplierSide.connectPushSupplier(supplier);
      consumerSide.connectPushConsumer(consumer);
      supplierSide.push(event);

      assertSame(event, consumer.lastEvent);
      assertTrue(supplierSide.isConnected());
      assertTrue(consumerSide.isConnected());
    }
  }

  @Test
  void disconnectsPushCallbacksIdempotently() {
    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel channel = service.createChannel();
      LocalPushConsumerProxy supplierSide = channel.supplierAdmin().obtainPushConsumerProxy();
      LocalPushSupplierProxy consumerSide = channel.consumerAdmin().obtainPushSupplierProxy();
      RecordingPushSupplier supplier = new RecordingPushSupplier();
      RecordingPushConsumer consumer = new RecordingPushConsumer();

      supplierSide.connectPushSupplier(supplier);
      consumerSide.connectPushConsumer(consumer);
      supplierSide.disconnectPushSupplier();
      supplierSide.disconnectPushSupplier();
      consumerSide.disconnectPushConsumer();
      consumerSide.disconnectPushConsumer();

      assertFalse(supplierSide.isConnected());
      assertFalse(consumerSide.isConnected());
      assertEquals(1, supplier.disconnectCount);
      assertEquals(1, consumer.disconnectCount);
    }
  }

  @Test
  void pullsEventsFromConnectedPullSuppliers() {
    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel channel = service.createChannel();
      LocalPullConsumerProxy supplierSide = channel.supplierAdmin().obtainPullConsumerProxy();
      LocalPullSupplierProxy consumerSide = channel.consumerAdmin().obtainPullSupplierProxy();
      RecordingPullSupplier supplier = new RecordingPullSupplier();
      RecordingPullConsumer consumer = new RecordingPullConsumer();
      AnyValue<String> event = stringEvent("beta");

      supplier.events.add(event);
      supplierSide.connectPullSupplier(supplier);
      consumerSide.connectPullConsumer(consumer);

      assertEquals(Optional.of(event), consumerSide.tryPull());
      supplier.events.add(event);
      assertSame(event, consumerSide.pull());
      assertTrue(supplierSide.isConnected());
      assertTrue(consumerSide.isConnected());
    }
  }

  @Test
  void disconnectsPullCallbacksIdempotently() {
    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel channel = service.createChannel();
      LocalPullConsumerProxy supplierSide = channel.supplierAdmin().obtainPullConsumerProxy();
      LocalPullSupplierProxy consumerSide = channel.consumerAdmin().obtainPullSupplierProxy();
      RecordingPullSupplier supplier = new RecordingPullSupplier();
      RecordingPullConsumer consumer = new RecordingPullConsumer();

      supplierSide.connectPullSupplier(supplier);
      consumerSide.connectPullConsumer(consumer);
      supplierSide.disconnectPullSupplier();
      supplierSide.disconnectPullSupplier();
      consumerSide.disconnectPullConsumer();
      consumerSide.disconnectPullConsumer();

      assertFalse(supplierSide.isConnected());
      assertFalse(consumerSide.isConnected());
      assertEquals(1, supplier.disconnectCount);
      assertEquals(1, consumer.disconnectCount);
    }
  }

  @Test
  void reportsEmptyPullDeterministically() {
    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel channel = service.createChannel();
      LocalPullConsumerProxy supplierSide = channel.supplierAdmin().obtainPullConsumerProxy();
      LocalPullSupplierProxy consumerSide = channel.consumerAdmin().obtainPullSupplierProxy();

      supplierSide.connectPullSupplier(new RecordingPullSupplier());
      consumerSide.connectPullConsumer(new RecordingPullConsumer());
      EventServiceException exception =
          assertThrows(EventServiceException.class, consumerSide::pull);

      assertEquals(Optional.empty(), consumerSide.tryPull());
      assertEquals(EventServiceDiagnosticCodes.NO_EVENT_AVAILABLE, exception.code());
    }
  }

  @Test
  void rejectsDisconnectedDeliveryOperations() {
    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel channel = service.createChannel();
      LocalPushConsumerProxy pushProxy = channel.supplierAdmin().obtainPushConsumerProxy();
      LocalPullSupplierProxy pullProxy = channel.consumerAdmin().obtainPullSupplierProxy();

      EventServiceException push =
          assertThrows(EventServiceException.class, () -> pushProxy.push(stringEvent("gamma")));
      EventServiceException pull = assertThrows(EventServiceException.class, pullProxy::tryPull);

      assertEquals(EventServiceDiagnosticCodes.PROXY_NOT_CONNECTED, push.code());
      assertEquals(EventServiceDiagnosticCodes.PROXY_NOT_CONNECTED, pull.code());
    }
  }

  @Test
  void enforcesConfiguredSupplierAndConsumerProxyLimits() {
    try (LocalEventService service =
        LocalEventService.create(new EventServiceOptions(1, 1, 1, 4))) {
      LocalEventChannel channel = service.createChannel();

      channel.supplierAdmin().obtainPushConsumerProxy();
      channel.consumerAdmin().obtainPushSupplierProxy();
      EventServiceException supplierLimit =
          assertThrows(
              EventServiceException.class, () -> channel.supplierAdmin().obtainPullConsumerProxy());
      EventServiceException consumerLimit =
          assertThrows(
              EventServiceException.class, () -> channel.consumerAdmin().obtainPullSupplierProxy());

      assertEquals(EventServiceDiagnosticCodes.SUPPLIER_LIMIT_EXCEEDED, supplierLimit.code());
      assertEquals(EventServiceDiagnosticCodes.CONSUMER_LIMIT_EXCEEDED, consumerLimit.code());
    }
  }

  @Test
  void freesProxyCapacityAfterDestroy() {
    try (LocalEventService service =
        LocalEventService.create(new EventServiceOptions(1, 1, 1, 4))) {
      LocalEventChannel channel = service.createChannel();
      LocalPushConsumerProxy supplierSide = channel.supplierAdmin().obtainPushConsumerProxy();
      LocalPushSupplierProxy consumerSide = channel.consumerAdmin().obtainPushSupplierProxy();

      supplierSide.destroy();
      consumerSide.destroy();

      assertProxy(
          channel.supplierAdmin().obtainPullConsumerProxy(),
          channel.id(),
          3L,
          EventProxyKind.PULL_CONSUMER);
      assertProxy(
          channel.consumerAdmin().obtainPullSupplierProxy(),
          channel.id(),
          4L,
          EventProxyKind.PULL_SUPPLIER);
    }
  }

  @Test
  void reportsStaleProxyAfterDestroy() {
    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel channel = service.createChannel();
      LocalPushConsumerProxy proxy = channel.supplierAdmin().obtainPushConsumerProxy();

      proxy.destroy();
      EventServiceException exception =
          assertThrows(
              EventServiceException.class,
              () -> proxy.connectPushSupplier(new RecordingPushSupplier()));

      assertEquals(EventServiceDiagnosticCodes.PROXY_DESTROYED, exception.code());
    }
  }

  @Test
  void enforcesPendingEventFanoutCapacity() {
    try (LocalEventService service =
        LocalEventService.create(new EventServiceOptions(1, 2, 2, 1))) {
      LocalEventChannel channel = service.createChannel();
      LocalPushConsumerProxy supplierSide = channel.supplierAdmin().obtainPushConsumerProxy();
      LocalPushSupplierProxy first = channel.consumerAdmin().obtainPushSupplierProxy();
      LocalPushSupplierProxy second = channel.consumerAdmin().obtainPushSupplierProxy();

      supplierSide.connectPushSupplier(new RecordingPushSupplier());
      first.connectPushConsumer(new RecordingPushConsumer());
      second.connectPushConsumer(new RecordingPushConsumer());
      EventServiceException exception =
          assertThrows(
              EventServiceException.class, () -> supplierSide.push(stringEvent("overflow")));

      assertEquals(EventServiceDiagnosticCodes.EVENT_QUEUE_FULL, exception.code());
    }
  }

  @Test
  void pendingEventFanoutIgnoresDisconnectedConsumers() {
    try (LocalEventService service =
        LocalEventService.create(new EventServiceOptions(1, 3, 3, 1))) {
      LocalEventChannel channel = service.createChannel();
      LocalPushConsumerProxy supplierSide = channel.supplierAdmin().obtainPushConsumerProxy();
      LocalPushSupplierProxy disconnectedOne = channel.consumerAdmin().obtainPushSupplierProxy();
      LocalPushSupplierProxy disconnectedTwo = channel.consumerAdmin().obtainPushSupplierProxy();
      LocalPushSupplierProxy connected = channel.consumerAdmin().obtainPushSupplierProxy();
      RecordingPushConsumer consumer = new RecordingPushConsumer();

      supplierSide.connectPushSupplier(new RecordingPushSupplier());
      connected.connectPushConsumer(consumer);
      supplierSide.push(stringEvent("accepted"));

      assertFalse(disconnectedOne.isConnected());
      assertFalse(disconnectedTwo.isConnected());
      assertEquals("accepted", consumer.lastEvent.value());
    }
  }

  @Test
  void removesFailingPushConsumersDeterministically() {
    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel channel = service.createChannel();
      LocalPushConsumerProxy supplierSide = channel.supplierAdmin().obtainPushConsumerProxy();
      LocalPushSupplierProxy failing = channel.consumerAdmin().obtainPushSupplierProxy();
      LocalPushSupplierProxy healthy = channel.consumerAdmin().obtainPushSupplierProxy();
      RecordingPushConsumer consumer = new RecordingPushConsumer();

      supplierSide.connectPushSupplier(new RecordingPushSupplier());
      failing.connectPushConsumer(new FailingPushConsumer());
      healthy.connectPushConsumer(consumer);
      EventServiceException first =
          assertThrows(EventServiceException.class, () -> supplierSide.push(stringEvent("first")));
      supplierSide.push(stringEvent("second"));
      EventServiceException stale =
          assertThrows(
              EventServiceException.class,
              () -> failing.connectPushConsumer(new RecordingPushConsumer()));

      assertEquals(EventServiceDiagnosticCodes.CONSUMER_DELIVERY_FAILED, first.code());
      assertEquals(EventServiceDiagnosticCodes.STALE_PROXY, stale.code());
      assertEquals("second", consumer.lastEvent.value());
    }
  }

  @Test
  void rejectsNullPayloads() {
    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel channel = service.createChannel();
      LocalPushConsumerProxy pushProxy = channel.supplierAdmin().obtainPushConsumerProxy();

      pushProxy.connectPushSupplier(new RecordingPushSupplier());
      EventServiceException nullPush =
          assertThrows(EventServiceException.class, () -> pushProxy.push(null));

      assertEquals(EventServiceDiagnosticCodes.INVALID_PAYLOAD, nullPush.code());
    }
  }

  @Test
  void reportsDestroyedChannelDuringDelivery() {
    try (LocalEventService service = LocalEventService.create()) {
      LocalEventChannel channel = service.createChannel();
      LocalPushConsumerProxy pushProxy = channel.supplierAdmin().obtainPushConsumerProxy();
      pushProxy.connectPushSupplier(new RecordingPushSupplier());
      channel.destroy();

      EventServiceException exception =
          assertThrows(EventServiceException.class, () -> pushProxy.push(stringEvent("delta")));

      assertEquals(EventServiceDiagnosticCodes.CHANNEL_DESTROYED, exception.code());
    }
  }

  private static void assertProxy(
      LocalEventProxy proxy, long channelId, long proxyId, EventProxyKind kind) {
    assertEquals(channelId, proxy.channelId());
    assertEquals(proxyId, proxy.id());
    assertEquals(kind, proxy.kind());
    assertFalse(proxy.isDestroyed());
  }

  private static AnyValue<String> stringEvent(String value) {
    return new AnyValue<>(IdlTypeCode.STRING, value);
  }

  private static final class RecordingPushConsumer implements EventPushConsumer {
    private AnyValue<?> lastEvent;
    private int disconnectCount;

    @Override
    public void push(AnyValue<?> event) {
      lastEvent = event;
    }

    @Override
    public void disconnectPushConsumer() {
      disconnectCount++;
    }
  }

  private static final class FailingPushConsumer implements EventPushConsumer {
    @Override
    public void push(AnyValue<?> event) {
      throw new IllegalStateException("consumer failed");
    }

    @Override
    public void disconnectPushConsumer() {}
  }

  private static final class RecordingPushSupplier implements EventPushSupplier {
    private int disconnectCount;

    @Override
    public void disconnectPushSupplier() {
      disconnectCount++;
    }
  }

  private static final class RecordingPullSupplier implements EventPullSupplier {
    private final ArrayDeque<AnyValue<?>> events = new ArrayDeque<>();
    private int disconnectCount;

    @Override
    public Optional<AnyValue<?>> pull() {
      return Optional.ofNullable(events.pollFirst());
    }

    @Override
    public Optional<AnyValue<?>> tryPull() {
      return Optional.ofNullable(events.pollFirst());
    }

    @Override
    public void disconnectPullSupplier() {
      disconnectCount++;
    }
  }

  private static final class RecordingPullConsumer implements EventPullConsumer {
    private int disconnectCount;

    @Override
    public void disconnectPullConsumer() {
      disconnectCount++;
    }
  }
}
