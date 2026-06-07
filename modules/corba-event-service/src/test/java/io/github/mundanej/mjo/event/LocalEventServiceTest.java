package io.github.mundanej.mjo.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  private static void assertProxy(
      LocalEventProxy proxy, long channelId, long proxyId, EventProxyKind kind) {
    assertEquals(channelId, proxy.channelId());
    assertEquals(proxyId, proxy.id());
    assertEquals(kind, proxy.kind());
    assertFalse(proxy.isDestroyed());
  }
}
