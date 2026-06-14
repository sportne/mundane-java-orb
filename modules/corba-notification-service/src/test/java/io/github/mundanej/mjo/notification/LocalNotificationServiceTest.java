package io.github.mundanej.mjo.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.event.EventProxyKind;
import org.junit.jupiter.api.Test;

final class LocalNotificationServiceTest {

  @Test
  void rejectsInvalidOptionBounds() {
    NotificationServiceException low =
        assertThrows(
            NotificationServiceException.class, () -> new NotificationServiceOptions(0, 1, 1));
    NotificationServiceException high =
        assertThrows(
            NotificationServiceException.class, () -> new NotificationServiceOptions(1, 65_536, 1));

    assertEquals(NotificationServiceDiagnosticCodes.INVALID_LIMIT, low.code());
    assertEquals(NotificationServiceDiagnosticCodes.INVALID_LIMIT, high.code());
  }

  @Test
  void createsAndDestroysLocalChannels() {
    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel first = service.createChannel();
      LocalNotificationChannel second = service.createChannel();

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
  void exposesStableEventCompatibilityBoundary() {
    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel channel = service.createChannel();
      NotificationEventCompatibility compatibility = channel.eventCompatibility();

      assertSame(service.eventCompatibility(), compatibility);
      assertEquals("CosNotification::EventChannel", compatibility.notificationService());
      assertEquals("CosEventChannelAdmin::EventChannel", compatibility.eventService());
      assertFalse(compatibility.deliveryCompatible());
    }
  }

  @Test
  void rejectsDuplicateChannelDestroyAndDestroyedAdminUse() {
    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel channel = service.createChannel();
      LocalNotificationSupplierAdmin supplierAdmin = channel.supplierAdmin();
      channel.destroy();

      NotificationServiceException duplicate =
          assertThrows(NotificationServiceException.class, channel::destroy);
      NotificationServiceException adminUse =
          assertThrows(
              NotificationServiceException.class, supplierAdmin::obtainStructuredPushConsumerProxy);

      assertEquals(NotificationServiceDiagnosticCodes.CHANNEL_DESTROYED, duplicate.code());
      assertEquals(NotificationServiceDiagnosticCodes.CHANNEL_DESTROYED, adminUse.code());
    }
  }

  @Test
  void rejectsCreateAfterServiceShutdown() {
    LocalNotificationService service = LocalNotificationService.create();
    service.close();

    NotificationServiceException exception =
        assertThrows(NotificationServiceException.class, service::createChannel);

    assertEquals(NotificationServiceDiagnosticCodes.SERVICE_SHUTDOWN, exception.code());
  }

  @Test
  void enforcesConfiguredChannelLimit() {
    try (LocalNotificationService service =
        LocalNotificationService.create(new NotificationServiceOptions(1, 1, 1))) {
      service.createChannel();

      NotificationServiceException exception =
          assertThrows(NotificationServiceException.class, service::createChannel);

      assertEquals(NotificationServiceDiagnosticCodes.CHANNEL_LIMIT_EXCEEDED, exception.code());
    }
  }

  @Test
  void createsAdminOwnedProxyHandlesWithEventCompatibilityRoles() {
    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel channel = service.createChannel();
      LocalNotificationSupplierAdmin supplierAdmin = channel.supplierAdmin();
      LocalNotificationConsumerAdmin consumerAdmin = channel.consumerAdmin();

      LocalStructuredPushConsumerProxy pushConsumer =
          supplierAdmin.obtainStructuredPushConsumerProxy();
      LocalStructuredPullConsumerProxy pullConsumer =
          supplierAdmin.obtainStructuredPullConsumerProxy();
      LocalStructuredPushSupplierProxy pushSupplier =
          consumerAdmin.obtainStructuredPushSupplierProxy();
      LocalStructuredPullSupplierProxy pullSupplier =
          consumerAdmin.obtainStructuredPullSupplierProxy();

      assertEquals(channel.id(), supplierAdmin.channelId());
      assertEquals(channel.id(), consumerAdmin.channelId());
      assertProxy(
          pushConsumer,
          channel.id(),
          1L,
          NotificationProxyKind.STRUCTURED_PUSH_CONSUMER,
          EventProxyKind.PUSH_CONSUMER);
      assertProxy(
          pullConsumer,
          channel.id(),
          2L,
          NotificationProxyKind.STRUCTURED_PULL_CONSUMER,
          EventProxyKind.PULL_CONSUMER);
      assertProxy(
          pushSupplier,
          channel.id(),
          3L,
          NotificationProxyKind.STRUCTURED_PUSH_SUPPLIER,
          EventProxyKind.PUSH_SUPPLIER);
      assertProxy(
          pullSupplier,
          channel.id(),
          4L,
          NotificationProxyKind.STRUCTURED_PULL_SUPPLIER,
          EventProxyKind.PULL_SUPPLIER);
    }
  }

  @Test
  void rejectsDuplicateProxyDestroy() {
    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel channel = service.createChannel();
      LocalStructuredPushConsumerProxy proxy =
          channel.supplierAdmin().obtainStructuredPushConsumerProxy();
      proxy.destroy();

      NotificationServiceException exception =
          assertThrows(NotificationServiceException.class, proxy::destroy);

      assertTrue(proxy.isDestroyed());
      assertEquals(NotificationServiceDiagnosticCodes.PROXY_DESTROYED, exception.code());
    }
  }

  @Test
  void enforcesConfiguredSupplierAndConsumerProxyLimits() {
    try (LocalNotificationService service =
        LocalNotificationService.create(new NotificationServiceOptions(1, 1, 1))) {
      LocalNotificationChannel channel = service.createChannel();

      channel.supplierAdmin().obtainStructuredPushConsumerProxy();
      channel.consumerAdmin().obtainStructuredPushSupplierProxy();
      NotificationServiceException supplierLimit =
          assertThrows(
              NotificationServiceException.class,
              () -> channel.supplierAdmin().obtainStructuredPullConsumerProxy());
      NotificationServiceException consumerLimit =
          assertThrows(
              NotificationServiceException.class,
              () -> channel.consumerAdmin().obtainStructuredPullSupplierProxy());

      assertEquals(
          NotificationServiceDiagnosticCodes.SUPPLIER_LIMIT_EXCEEDED, supplierLimit.code());
      assertEquals(
          NotificationServiceDiagnosticCodes.CONSUMER_LIMIT_EXCEEDED, consumerLimit.code());
    }
  }

  @Test
  void freesProxyCapacityAfterDestroy() {
    try (LocalNotificationService service =
        LocalNotificationService.create(new NotificationServiceOptions(1, 1, 1))) {
      LocalNotificationChannel channel = service.createChannel();
      LocalStructuredPushConsumerProxy supplierSide =
          channel.supplierAdmin().obtainStructuredPushConsumerProxy();
      LocalStructuredPushSupplierProxy consumerSide =
          channel.consumerAdmin().obtainStructuredPushSupplierProxy();

      supplierSide.destroy();
      consumerSide.destroy();

      assertProxy(
          channel.supplierAdmin().obtainStructuredPullConsumerProxy(),
          channel.id(),
          3L,
          NotificationProxyKind.STRUCTURED_PULL_CONSUMER,
          EventProxyKind.PULL_CONSUMER);
      assertProxy(
          channel.consumerAdmin().obtainStructuredPullSupplierProxy(),
          channel.id(),
          4L,
          NotificationProxyKind.STRUCTURED_PULL_SUPPLIER,
          EventProxyKind.PULL_SUPPLIER);
    }
  }

  @Test
  void reportsDestroyedChannelDuringProxyUse() {
    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel channel = service.createChannel();
      LocalStructuredPushConsumerProxy proxy =
          channel.supplierAdmin().obtainStructuredPushConsumerProxy();
      channel.destroy();

      NotificationServiceException exception =
          assertThrows(NotificationServiceException.class, proxy::requireAlive);

      assertEquals(NotificationServiceDiagnosticCodes.CHANNEL_DESTROYED, exception.code());
    }
  }

  private static void assertProxy(
      LocalNotificationProxy proxy,
      long channelId,
      long proxyId,
      NotificationProxyKind kind,
      EventProxyKind eventProxyKind) {
    assertEquals(channelId, proxy.channelId());
    assertEquals(proxyId, proxy.id());
    assertEquals(kind, proxy.kind());
    assertEquals(eventProxyKind, proxy.eventProxyKind());
    assertFalse(proxy.isDestroyed());
  }
}
