package io.github.mundanej.mjo.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class LocalNotificationDeliveryTest {

  @Test
  void deliversStructuredPushEventsToConnectedPushConsumers() {
    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel channel = service.createChannel();
      LocalStructuredPushConsumerProxy supplierSide =
          channel.supplierAdmin().obtainStructuredPushConsumerProxy();
      LocalStructuredPushSupplierProxy consumerSide =
          channel.consumerAdmin().obtainStructuredPushSupplierProxy();
      RecordingPushConsumer consumer = new RecordingPushConsumer();
      NotificationStructuredEvent event = event("market", "quote", "opened", "AAPL");

      supplierSide.connectStructuredPushSupplier(new NoopPushSupplier());
      consumerSide.connectStructuredPushConsumer(consumer);
      supplierSide.pushStructuredEvent(event);

      assertEquals(List.of(event), consumer.events);
    }
  }

  @Test
  void appliesFiltersToPushAndPullConsumers() {
    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel channel = service.createChannel();
      LocalStructuredPushConsumerProxy supplierSide =
          channel.supplierAdmin().obtainStructuredPushConsumerProxy();
      LocalStructuredPushSupplierProxy pushTarget =
          channel.consumerAdmin().obtainStructuredPushSupplierProxy();
      LocalStructuredPullSupplierProxy pullTarget =
          channel.consumerAdmin().obtainStructuredPullSupplierProxy();
      RecordingPushConsumer pushConsumer = new RecordingPushConsumer();
      NotificationFilter onlyAapl = NotificationFilter.parse("filter.symbol == 'AAPL'");
      NotificationStructuredEvent accepted = event("market", "quote", "opened", "AAPL");
      NotificationStructuredEvent rejected = event("market", "quote", "opened", "MSFT");

      supplierSide.connectStructuredPushSupplier(new NoopPushSupplier());
      pushTarget.connectStructuredPushConsumer(pushConsumer, onlyAapl);
      pullTarget.connectStructuredPullConsumer(new NoopPullConsumer(), onlyAapl);

      supplierSide.pushStructuredEvent(rejected);
      assertTrue(pullTarget.tryPullStructuredEvent().isEmpty());

      supplierSide.pushStructuredEvent(accepted);

      assertEquals(List.of(accepted), pushConsumer.events);
      assertEquals(Optional.of(accepted), pullTarget.tryPullStructuredEvent());
    }
  }

  @Test
  void queuesStructuredEventsForPullConsumersWithBoundedCapacity() {
    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel channel = service.createChannel();
      LocalStructuredPushConsumerProxy supplierSide =
          channel.supplierAdmin().obtainStructuredPushConsumerProxy();
      LocalStructuredPullSupplierProxy consumerSide =
          channel.consumerAdmin().obtainStructuredPullSupplierProxy();
      NotificationPolicies oneEventQueue =
          NotificationPolicies.from(
              List.of(NotificationPolicyProperty.signedLong("queue-limit", 1)));
      NotificationStructuredEvent first = event("market", "quote", "opened", "AAPL");
      NotificationStructuredEvent second = event("market", "quote", "opened", "MSFT");

      supplierSide.connectStructuredPushSupplier(new NoopPushSupplier());
      consumerSide.connectStructuredPullConsumer(
          new NoopPullConsumer(), NotificationFilter.parse("true"), oneEventQueue);
      supplierSide.pushStructuredEvent(first);

      NotificationServiceException full =
          assertThrows(
              NotificationServiceException.class, () -> supplierSide.pushStructuredEvent(second));

      assertEquals(NotificationServiceDiagnosticCodes.EVENT_QUEUE_FULL, full.code());
      assertEquals(Optional.of(first), consumerSide.tryPullStructuredEvent());
      assertTrue(consumerSide.tryPullStructuredEvent().isEmpty());
    }
  }

  @Test
  void enforcesConnectedProxyFanoutLimit() {
    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel channel = service.createChannel();
      LocalStructuredPushConsumerProxy supplierSide =
          channel.supplierAdmin().obtainStructuredPushConsumerProxy();
      LocalStructuredPushSupplierProxy pushTarget =
          channel.consumerAdmin().obtainStructuredPushSupplierProxy();
      LocalStructuredPullSupplierProxy pullTarget =
          channel.consumerAdmin().obtainStructuredPullSupplierProxy();
      NotificationPolicies oneTargetPolicy =
          NotificationPolicies.from(
              List.of(
                  NotificationPolicyProperty.signedLong("supplier-admin-limit", 1),
                  NotificationPolicyProperty.signedLong("consumer-admin-limit", 1),
                  NotificationPolicyProperty.signedLong("proxy-limit", 1)));

      supplierSide.connectStructuredPushSupplier(new NoopPushSupplier());
      pushTarget.connectStructuredPushConsumer(
          new RecordingPushConsumer(), NotificationFilter.parse("true"), oneTargetPolicy);
      pullTarget.connectStructuredPullConsumer(
          new NoopPullConsumer(), NotificationFilter.parse("true"), oneTargetPolicy);

      NotificationServiceException exception =
          assertThrows(
              NotificationServiceException.class,
              () -> supplierSide.pushStructuredEvent(event("market", "quote", "opened", "AAPL")));

      assertEquals(NotificationServiceDiagnosticCodes.EVENT_QUEUE_FULL, exception.code());
    }
  }

  @Test
  void enforcesConnectedFilterPolicyLimits() {
    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel channel = service.createChannel();
      LocalStructuredPushSupplierProxy pushTarget =
          channel.consumerAdmin().obtainStructuredPushSupplierProxy();
      LocalStructuredPullSupplierProxy pullTarget =
          channel.consumerAdmin().obtainStructuredPullSupplierProxy();
      NotificationPolicies shortFilter =
          NotificationPolicies.from(
              List.of(NotificationPolicyProperty.signedLong("filter-length-limit", 3)));
      NotificationPolicies shallowFilter =
          NotificationPolicies.from(
              List.of(NotificationPolicyProperty.signedLong("filter-depth-limit", 1)));
      NotificationPolicies oneTermFilter =
          NotificationPolicies.from(
              List.of(NotificationPolicyProperty.signedLong("filter-term-limit", 1)));

      NotificationServiceException length =
          assertThrows(
              NotificationServiceException.class,
              () ->
                  pushTarget.connectStructuredPushConsumer(
                      new RecordingPushConsumer(), NotificationFilter.parse("true"), shortFilter));
      NotificationServiceException depth =
          assertThrows(
              NotificationServiceException.class,
              () ->
                  pullTarget.connectStructuredPullConsumer(
                      new NoopPullConsumer(), NotificationFilter.parse("((true))"), shallowFilter));
      NotificationServiceException terms =
          assertThrows(
              NotificationServiceException.class,
              () ->
                  pushTarget.connectStructuredPushConsumer(
                      new RecordingPushConsumer(),
                      NotificationFilter.parse("true and true"),
                      oneTermFilter));

      assertEquals(NotificationServiceDiagnosticCodes.FILTER_LIMIT_EXCEEDED, length.code());
      assertEquals(NotificationServiceDiagnosticCodes.FILTER_LIMIT_EXCEEDED, depth.code());
      assertEquals(NotificationServiceDiagnosticCodes.FILTER_LIMIT_EXCEEDED, terms.code());
    }
  }

  @Test
  void pullsStructuredEventsFromConnectedPullSuppliers() {
    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel channel = service.createChannel();
      LocalStructuredPullConsumerProxy supplierSide =
          channel.supplierAdmin().obtainStructuredPullConsumerProxy();
      LocalStructuredPullSupplierProxy consumerSide =
          channel.consumerAdmin().obtainStructuredPullSupplierProxy();
      NotificationStructuredEvent event = event("audit", "entry", "created", "MJO");

      supplierSide.connectStructuredPullSupplier(new QueueingPullSupplier(event));
      consumerSide.connectStructuredPullConsumer(new NoopPullConsumer());

      assertEquals(event, consumerSide.pullStructuredEvent());
      assertTrue(consumerSide.tryPullStructuredEvent().isEmpty());
    }
  }

  @Test
  void reportsFailedPushConsumersAndLeavesProxyStale() {
    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel channel = service.createChannel();
      LocalStructuredPushConsumerProxy supplierSide =
          channel.supplierAdmin().obtainStructuredPushConsumerProxy();
      LocalStructuredPushSupplierProxy failingTarget =
          channel.consumerAdmin().obtainStructuredPushSupplierProxy();

      supplierSide.connectStructuredPushSupplier(new NoopPushSupplier());
      failingTarget.connectStructuredPushConsumer(
          new NotificationPushConsumer() {
            @Override
            public void pushStructuredEvent(NotificationStructuredEvent event) {
              throw new IllegalStateException("consumer failed");
            }

            @Override
            public void disconnectStructuredPushConsumer() {}
          });

      NotificationServiceException failed =
          assertThrows(
              NotificationServiceException.class,
              () -> supplierSide.pushStructuredEvent(event("audit", "entry", "created", "MJO")));
      NotificationServiceException stale =
          assertThrows(
              NotificationServiceException.class,
              () -> failingTarget.connectStructuredPushConsumer(new RecordingPushConsumer()));

      assertEquals(NotificationServiceDiagnosticCodes.CONSUMER_DELIVERY_FAILED, failed.code());
      assertEquals(NotificationServiceDiagnosticCodes.STALE_PROXY, stale.code());
    }
  }

  @Test
  void reportsDestroyedChannelDuringDelivery() {
    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel channel = service.createChannel();
      LocalStructuredPushConsumerProxy supplierSide =
          channel.supplierAdmin().obtainStructuredPushConsumerProxy();
      supplierSide.connectStructuredPushSupplier(new NoopPushSupplier());
      channel.destroy();

      NotificationServiceException exception =
          assertThrows(
              NotificationServiceException.class,
              () -> supplierSide.pushStructuredEvent(event("audit", "entry", "created", "MJO")));

      assertEquals(NotificationServiceDiagnosticCodes.CHANNEL_DESTROYED, exception.code());
    }
  }

  private static NotificationStructuredEvent event(
      String domain, String type, String name, String symbol) {
    return new NotificationStructuredEvent(
        new NotificationEventIdentity(new NotificationEventType(domain, type), name),
        List.of(NotificationProperty.stringProperty("symbol", symbol)),
        List.of(),
        List.of());
  }

  private static final class RecordingPushConsumer implements NotificationPushConsumer {
    private final List<NotificationStructuredEvent> events = new ArrayList<>();

    @Override
    public void pushStructuredEvent(NotificationStructuredEvent event) {
      events.add(event);
    }

    @Override
    public void disconnectStructuredPushConsumer() {}
  }

  private static final class NoopPushSupplier implements NotificationPushSupplier {
    @Override
    public void disconnectStructuredPushSupplier() {}
  }

  private static final class NoopPullConsumer implements NotificationPullConsumer {
    @Override
    public void disconnectStructuredPullConsumer() {}
  }

  private static final class QueueingPullSupplier implements NotificationPullSupplier {
    private final ArrayDeque<NotificationStructuredEvent> events = new ArrayDeque<>();

    private QueueingPullSupplier(NotificationStructuredEvent event) {
      events.add(event);
    }

    @Override
    public Optional<NotificationStructuredEvent> pullStructuredEvent() {
      return Optional.ofNullable(events.pollFirst());
    }

    @Override
    public Optional<NotificationStructuredEvent> tryPullStructuredEvent() {
      return Optional.empty();
    }

    @Override
    public void disconnectStructuredPullSupplier() {}
  }
}
