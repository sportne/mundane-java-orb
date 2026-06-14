package io.github.mundanej.mjo.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.giop.GiopSystemExceptionBody;
import io.github.mundanej.mjo.iiop.IiopClient;
import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopObjectReference;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.naming.server.NetworkNamingClient;
import io.github.mundanej.mjo.naming.server.NetworkNamingService;
import io.github.mundanej.mjo.naming.server.RemoteNamingBindingTarget;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Loopback IIOP and Naming tests for the supported Notification Service subset. */
@Tag("unit")
final class NetworkNotificationServiceTest {

  @Test
  void notificationChannelAdminProxyPushAndPullDispatchOverIiop() {
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
      QueuePullSupplier supplier = new QueuePullSupplier(event("audit", "entry", "pulled", "MJO"));
      service
          .channel()
          .supplierAdmin()
          .obtainStructuredPullConsumerProxy()
          .connectStructuredPullSupplier(supplier);

      var eventChannel = service.ior();
      var supplierAdmin = client.forSuppliers(eventChannel);
      var consumerAdmin = client.forConsumers(eventChannel);
      var pushConsumer = client.obtainStructuredPushConsumer(supplierAdmin);
      var pullConsumer = client.obtainStructuredPullConsumer(supplierAdmin);
      var pushSupplier = client.obtainStructuredPushSupplier(consumerAdmin);
      var pullSupplier = client.obtainStructuredPullSupplier(consumerAdmin);
      NotificationStructuredEvent pushed = event("audit", "entry", "pushed", "MJO");

      client.pushStructuredEvent(pushConsumer, pushed);

      assertEquals(pushed, consumer.lastEvent);
      assertEquals(pushed, client.pullStructuredEvent(pullSupplier));
      assertEquals(
          event("audit", "entry", "pulled", "MJO"), client.pullStructuredEvent(pullSupplier));
      assertFalse(client.tryPullStructuredEvent(pullSupplier).isPresent());
      client.disconnectStructuredPullConsumer(pullConsumer);
      client.disconnectStructuredPushSupplier(pushSupplier);
      client.disconnectStructuredPullSupplier(pullSupplier);
      assertEquals(
          "IDL:omg.org/CosNotifyChannelAdmin/EventChannel:1.0",
          service.objectReference().ior().typeId());
      assertEquals(
          NetworkNotificationService.DEFAULT_OBJECT_ID,
          new String(service.objectReference().objectKey(), StandardCharsets.US_ASCII));
    }
  }

  @Test
  void notificationChannelIorCanBeBoundAndResolvedThroughNetworkNaming() {
    NetworkNotificationServiceClient client =
        NetworkNotificationServiceClient.create(IiopOptions.defaults());

    try (NetworkNamingService naming =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkNamingClient namingClient =
            NetworkNamingClient.connect(naming.ior(), IiopOptions.defaults());
        NetworkNotificationService service =
            NetworkNotificationService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults())) {
      service.bindInNaming(namingClient, NamingName.parse("NotificationService"));
      RemoteNamingBindingTarget target =
          namingClient.resolve(NamingName.parse("NotificationService"));

      assertEquals(RemoteNamingBindingTarget.Kind.OBJECT, target.kind());
      var supplierAdmin = client.forSuppliers(target.ior());
      var pushConsumer = client.obtainStructuredPushConsumer(supplierAdmin);
      RecordingPushConsumer consumer = new RecordingPushConsumer();
      service
          .channel()
          .consumerAdmin()
          .obtainStructuredPushSupplierProxy()
          .connectStructuredPushConsumer(consumer);

      client.pushStructuredEvent(pushConsumer, event("audit", "entry", "named", "MJO"));

      assertEquals(event("audit", "entry", "named", "MJO"), consumer.lastEvent);
    }
  }

  @Test
  void unknownObjectKeyAndOperationReturnDeterministicSystemExceptions() {
    try (NetworkNotificationService service =
            NetworkNotificationService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        IiopClient rawClient = IiopClient.connect(service.endpoint(), IiopOptions.defaults())) {
      assertSystemException(
          "IDL:omg.org/CORBA/OBJECT_NOT_EXIST:1.0",
          rawClient.invoke(
              request(
                  1, "missing".getBytes(StandardCharsets.US_ASCII), "for_suppliers", new byte[0])));
      assertSystemException(
          "IDL:omg.org/CORBA/BAD_OPERATION:1.0",
          rawClient.invoke(
              request(2, service.objectReference().objectKey(), "missing", new byte[0])));
    }
  }

  @Test
  void invalidRequestBodiesDisconnectedProxiesAndPolicyRejectionMapToBadParamReplies() {
    NetworkNotificationServiceClient client =
        NetworkNotificationServiceClient.create(IiopOptions.defaults());

    try (NetworkNotificationService service =
            NetworkNotificationService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        IiopClient rawClient = IiopClient.connect(service.endpoint(), IiopOptions.defaults())) {
      var supplierAdmin = client.forSuppliers(service.ior());
      var consumerAdmin = client.forConsumers(service.ior());
      var pushConsumer = client.obtainStructuredPushConsumer(supplierAdmin);
      var pushSupplier = client.obtainStructuredPushSupplier(consumerAdmin);
      byte[] pushConsumerKey = IiopObjectReference.fromIor(pushConsumer).objectKey();
      byte[] pushSupplierKey = IiopObjectReference.fromIor(pushSupplier).objectKey();

      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(3, service.objectReference().objectKey(), "for_suppliers", new byte[] {1})));
      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(
                  4,
                  pushConsumerKey,
                  "push_structured_event",
                  CdrWriter.bigEndian().writeString("incomplete").toByteArray())));
      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(
                  5,
                  pushSupplierKey,
                  "set_filter",
                  NotificationServiceIiopCodec.INSTANCE.encodeArguments(
                      NotificationServiceDescriptors.SET_FILTER,
                      List.of("filter.symbol = 'MJO'")))));
      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(
                  6,
                  pushSupplierKey,
                  "set_integer_qos",
                  NotificationServiceIiopCodec.INSTANCE.encodeArguments(
                      NotificationServiceDescriptors.SET_INTEGER_QOS,
                      List.of("queue-limit", Long.valueOf(0L))))));
      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(
                  7,
                  pushSupplierKey,
                  "set_boolean_qos",
                  NotificationServiceIiopCodec.INSTANCE.encodeArguments(
                      NotificationServiceDescriptors.SET_BOOLEAN_QOS, List.of("durable", true)))));

      client.disconnectStructuredPushConsumer(pushConsumer);
      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(
                  8,
                  pushConsumerKey,
                  "push_structured_event",
                  NotificationServiceIiopCodec.INSTANCE.encodeArguments(
                      NotificationServiceDescriptors.PUSH_STRUCTURED_EVENT,
                      List.of(event("audit", "entry", "after-disconnect", "MJO"))))));
    }
  }

  @Test
  void codecRoundTripsStructuredEventsAndTryPullShape() {
    NotificationStructuredEvent value = event("market", "quote", "opened", "AAPL");

    byte[] request =
        NotificationServiceIiopCodec.INSTANCE.encodeArguments(
            NotificationServiceDescriptors.PUSH_STRUCTURED_EVENT, List.of(value));
    assertEquals(
        value,
        NotificationServiceIiopCodec.INSTANCE
            .decodeArguments(NotificationServiceDescriptors.PUSH_STRUCTURED_EVENT, request)
            .get(0));
    assertEquals(
        value,
        NotificationServiceIiopCodec.INSTANCE.decodeReturnValue(
            NotificationServiceDescriptors.PULL_STRUCTURED_EVENT,
            NotificationServiceIiopCodec.INSTANCE.encodeReturnValue(
                NotificationServiceDescriptors.PULL_STRUCTURED_EVENT, value)));
    NotificationTryPullResult result =
        (NotificationTryPullResult)
            NotificationServiceIiopCodec.INSTANCE.decodeReturnValue(
                NotificationServiceDescriptors.TRY_PULL_STRUCTURED_EVENT,
                NotificationServiceIiopCodec.INSTANCE.encodeReturnValue(
                    NotificationServiceDescriptors.TRY_PULL_STRUCTURED_EVENT,
                    NotificationTryPullResult.present(value)));

    assertEquals(Optional.of(value), result.event());
  }

  @Test
  void oldProxyIorAfterChannelDestroyMapsToDeterministicBadParam() {
    NetworkNotificationServiceClient client =
        NetworkNotificationServiceClient.create(IiopOptions.defaults());

    try (NetworkNotificationService service =
            NetworkNotificationService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        IiopClient rawClient = IiopClient.connect(service.endpoint(), IiopOptions.defaults())) {
      var supplierAdmin = client.forSuppliers(service.ior());
      var pushConsumer = client.obtainStructuredPushConsumer(supplierAdmin);
      byte[] pushKey = IiopObjectReference.fromIor(pushConsumer).objectKey();

      client.destroy(service.ior());

      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(
                  9,
                  pushKey,
                  "push_structured_event",
                  NotificationServiceIiopCodec.INSTANCE.encodeArguments(
                      NotificationServiceDescriptors.PUSH_STRUCTURED_EVENT,
                      List.of(event("audit", "entry", "stale", "MJO"))))));
    }
  }

  @Test
  void closeShutsDownLoopbackServer() {
    NetworkNotificationService service =
        NetworkNotificationService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
    var endpoint = service.endpoint();
    service.close();

    assertThrows(
        io.github.mundanej.mjo.iiop.IiopException.class,
        () -> io.github.mundanej.mjo.iiop.IiopClient.connect(endpoint, IiopOptions.defaults()));
  }

  private static GiopRequest request(
      long requestId, byte[] objectKey, String operation, byte[] body) {
    return new GiopRequest(
        GiopHeader.forType(GiopMessageType.REQUEST),
        requestId,
        3,
        objectKey,
        operation,
        List.of(),
        body);
  }

  private static void assertSystemException(String repositoryId, GiopReply reply) {
    assertEquals(GiopReplyStatus.SYSTEM_EXCEPTION, reply.replyStatus());
    GiopSystemExceptionBody body =
        GiopSystemExceptionBody.fromBytes(CdrByteOrder.BIG_ENDIAN, reply.body());
    assertEquals(repositoryId, body.repositoryId());
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
