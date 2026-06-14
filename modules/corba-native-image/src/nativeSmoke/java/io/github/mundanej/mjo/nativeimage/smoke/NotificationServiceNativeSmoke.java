package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.iiop.IiopClient;
import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopException;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.naming.server.NetworkNamingClient;
import io.github.mundanej.mjo.naming.server.NetworkNamingService;
import io.github.mundanej.mjo.naming.server.RemoteNamingBindingTarget;
import io.github.mundanej.mjo.notification.LocalNotificationChannel;
import io.github.mundanej.mjo.notification.LocalNotificationService;
import io.github.mundanej.mjo.notification.NetworkNotificationService;
import io.github.mundanej.mjo.notification.NetworkNotificationServiceClient;
import io.github.mundanej.mjo.notification.NotificationEventIdentity;
import io.github.mundanej.mjo.notification.NotificationEventType;
import io.github.mundanej.mjo.notification.NotificationFilter;
import io.github.mundanej.mjo.notification.NotificationPolicies;
import io.github.mundanej.mjo.notification.NotificationPolicyProperty;
import io.github.mundanej.mjo.notification.NotificationProperty;
import io.github.mundanej.mjo.notification.NotificationPullSupplier;
import io.github.mundanej.mjo.notification.NotificationPushConsumer;
import io.github.mundanej.mjo.notification.NotificationServiceDiagnosticCodes;
import io.github.mundanej.mjo.notification.NotificationServiceException;
import io.github.mundanej.mjo.notification.NotificationStructuredEvent;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;

/** Native Image smoke coverage for the supported local Notification Service slice. */
public final class NotificationServiceNativeSmoke {

  private NotificationServiceNativeSmoke() {}

  /** Runs the Notification Service Native Image smoke checks. */
  public static void main(String[] args) {
    NotificationStructuredEvent pushed = event("audit", "entry", "pushed", "MJO");
    NotificationStructuredEvent pulled = event("audit", "entry", "pulled", "MJO");

    try (LocalNotificationService service = LocalNotificationService.create()) {
      LocalNotificationChannel channel = service.createChannel();
      RecordingPushConsumer consumer = new RecordingPushConsumer();
      channel
          .consumerAdmin()
          .obtainStructuredPushSupplierProxy()
          .connectStructuredPushConsumer(
              consumer, NotificationFilter.parse("filter.symbol == 'MJO'"));
      var pushConsumer = channel.supplierAdmin().obtainStructuredPushConsumerProxy();
      pushConsumer.connectStructuredPushSupplier(() -> {});
      pushConsumer.pushStructuredEvent(pushed);
      SmokeAssertions.requireEquals(pushed, consumer.lastEvent, "local push delivery");

      var pullConsumer = channel.supplierAdmin().obtainStructuredPullConsumerProxy();
      pullConsumer.connectStructuredPullSupplier(new QueuePullSupplier(pulled));
      var pullSupplier = channel.consumerAdmin().obtainStructuredPullSupplierProxy();
      pullSupplier.connectStructuredPullConsumer(() -> {});
      SmokeAssertions.requireEquals(pulled, pullSupplier.pullStructuredEvent(), "local pull");
    }

    SmokeAssertions.require(
        NotificationFilter.parse("filter.symbol == 'MJO'").evaluate(pushed),
        "bounded filter evaluation");
    assertDiagnostic(
        NotificationServiceDiagnosticCodes.UNSUPPORTED_FILTER_OPERATOR,
        () -> NotificationFilter.parse("filter.symbol = 'MJO'"),
        "unsupported filter operator");
    assertDiagnostic(
        NotificationServiceDiagnosticCodes.UNSUPPORTED_POLICY,
        () -> NotificationPolicies.from(List.of(NotificationPolicyProperty.bool("durable", true))),
        "unsupported durable policy");

    NetworkNotificationServiceClient client =
        NetworkNotificationServiceClient.create(IiopOptions.defaults());
    try (NetworkNotificationService service =
        NetworkNotificationService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults())) {
      RecordingPushConsumer consumer = new RecordingPushConsumer();
      service
          .channel()
          .consumerAdmin()
          .obtainStructuredPushSupplierProxy()
          .connectStructuredPushConsumer(consumer);
      service
          .channel()
          .supplierAdmin()
          .obtainStructuredPullConsumerProxy()
          .connectStructuredPullSupplier(new QueuePullSupplier(pulled));

      var supplierAdmin = client.forSuppliers(service.ior());
      var consumerAdmin = client.forConsumers(service.ior());
      var pushConsumer = client.obtainStructuredPushConsumer(supplierAdmin);
      var pullSupplier = client.obtainStructuredPullSupplier(consumerAdmin);

      client.pushStructuredEvent(pushConsumer, pushed);
      SmokeAssertions.requireEquals(pushed, consumer.lastEvent, "IIOP push delivery");
      SmokeAssertions.requireEquals(pushed, client.pullStructuredEvent(pullSupplier), "IIOP queue");
      SmokeAssertions.requireEquals(pulled, client.pullStructuredEvent(pullSupplier), "IIOP pull");
      SmokeAssertions.require(
          client.tryPullStructuredEvent(pullSupplier).isEmpty(), "IIOP empty try_pull");
    }

    try (NetworkNamingService naming =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkNamingClient namingClient =
            NetworkNamingClient.connect(naming.ior(), IiopOptions.defaults());
        NetworkNotificationService service =
            NetworkNotificationService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults())) {
      service.bindInNaming(namingClient, NamingName.parse("NotificationService"));
      RemoteNamingBindingTarget target =
          namingClient.resolve(NamingName.parse("NotificationService"));
      var supplierAdmin = client.forSuppliers(target.ior());
      var pushConsumer = client.obtainStructuredPushConsumer(supplierAdmin);
      RecordingPushConsumer consumer = new RecordingPushConsumer();
      service
          .channel()
          .consumerAdmin()
          .obtainStructuredPushSupplierProxy()
          .connectStructuredPushConsumer(consumer);

      client.pushStructuredEvent(pushConsumer, event("audit", "entry", "named", "MJO"));
      SmokeAssertions.requireEquals(
          event("audit", "entry", "named", "MJO"), consumer.lastEvent, "Naming-resolved push");
    }

    NetworkNotificationService closed =
        NetworkNotificationService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
    IiopEndpoint endpoint = closed.endpoint();
    closed.close();
    SmokeAssertions.requireThrows(
        IiopException.class,
        () -> IiopClient.connect(endpoint, IiopOptions.defaults()),
        "Notification Service close shuts down server");
  }

  private static void assertDiagnostic(
      DiagnosticCode code, SmokeAssertions.ThrowingAction action, String label) {
    try {
      action.run();
    } catch (NotificationServiceException expected) {
      SmokeAssertions.requireEquals(code, expected.code(), label);
      return;
    } catch (Exception exception) {
      throw new AssertionError("Native Image smoke failed: " + label, exception);
    }
    throw new AssertionError("Native Image smoke failed: " + label + "; expected diagnostic");
  }

  private static NotificationStructuredEvent event(
      String domain, String type, String name, String symbol) {
    return new NotificationStructuredEvent(
        new NotificationEventIdentity(new NotificationEventType(domain, type), name),
        List.of(NotificationProperty.stringProperty("symbol", symbol)),
        List.of(NotificationProperty.signedLongProperty("sequence", 1L)),
        List.of(NotificationProperty.booleanProperty("active", true)));
  }

  private static final class RecordingPushConsumer implements NotificationPushConsumer {
    private NotificationStructuredEvent lastEvent;

    @Override
    public void pushStructuredEvent(NotificationStructuredEvent event) {
      lastEvent = event;
    }

    @Override
    public void disconnectStructuredPushConsumer() {}
  }

  private static final class QueuePullSupplier implements NotificationPullSupplier {
    private final ArrayDeque<NotificationStructuredEvent> events = new ArrayDeque<>();

    private QueuePullSupplier(NotificationStructuredEvent event) {
      events.add(event);
    }

    @Override
    public Optional<NotificationStructuredEvent> pullStructuredEvent() {
      return Optional.ofNullable(events.poll());
    }

    @Override
    public Optional<NotificationStructuredEvent> tryPullStructuredEvent() {
      return Optional.ofNullable(events.poll());
    }

    @Override
    public void disconnectStructuredPullSupplier() {}
  }
}
