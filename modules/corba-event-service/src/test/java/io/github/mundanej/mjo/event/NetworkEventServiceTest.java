package io.github.mundanej.mjo.event;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.any.AnyWireCodec;
import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrReader;
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
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Loopback IIOP and Naming tests for the supported Event Service subset. */
@Tag("unit")
final class NetworkEventServiceTest {

  @Test
  void eventChannelAdminProxyPushAndPullDispatchOverIiop() {
    NetworkEventServiceClient client = NetworkEventServiceClient.create(IiopOptions.defaults());

    try (NetworkEventService service =
        NetworkEventService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults())) {
      RecordingPushConsumer consumer = new RecordingPushConsumer();
      service.channel().consumerAdmin().obtainPushSupplierProxy().connectPushConsumer(consumer);
      QueuePullSupplier supplier = new QueuePullSupplier(stringEvent("pulled"));
      service.channel().supplierAdmin().obtainPullConsumerProxy().connectPullSupplier(supplier);

      var eventChannel = service.ior();
      var supplierAdmin = client.forSuppliers(eventChannel);
      var consumerAdmin = client.forConsumers(eventChannel);
      var pushConsumer = client.obtainPushConsumer(supplierAdmin);
      var pullConsumer = client.obtainPullConsumer(supplierAdmin);
      var pushSupplier = client.obtainPushSupplier(consumerAdmin);
      var pullSupplier = client.obtainPullSupplier(consumerAdmin);

      client.push(pushConsumer, stringEvent("pushed"));
      assertEquals(stringEvent("pushed"), consumer.lastEvent);
      assertEquals(stringEvent("pulled"), client.pull(pullSupplier));
      assertFalse(client.tryPull(pullSupplier).isPresent());
      client.disconnectPullConsumer(pullConsumer);
      client.disconnectPushSupplier(pushSupplier);
      client.disconnectPullSupplier(pullSupplier);
      assertEquals(
          "IDL:omg.org/CosEventChannelAdmin/EventChannel:1.0",
          service.objectReference().ior().typeId());
      assertArrayEquals(
          NetworkEventService.DEFAULT_OBJECT_ID.getBytes(StandardCharsets.US_ASCII),
          service.objectReference().objectKey());
    }
  }

  @Test
  void eventChannelIorCanBeBoundAndResolvedThroughNetworkNaming() {
    NetworkEventServiceClient client = NetworkEventServiceClient.create(IiopOptions.defaults());

    try (NetworkNamingService naming =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkNamingClient namingClient =
            NetworkNamingClient.connect(naming.ior(), IiopOptions.defaults());
        NetworkEventService service =
            NetworkEventService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults())) {
      service.bindInNaming(namingClient, NamingName.parse("EventService"));
      RemoteNamingBindingTarget target = namingClient.resolve(NamingName.parse("EventService"));

      assertEquals(RemoteNamingBindingTarget.Kind.OBJECT, target.kind());
      var supplierAdmin = client.forSuppliers(target.ior());
      var pushConsumer = client.obtainPushConsumer(supplierAdmin);
      RecordingPushConsumer consumer = new RecordingPushConsumer();
      service.channel().consumerAdmin().obtainPushSupplierProxy().connectPushConsumer(consumer);

      client.push(pushConsumer, stringEvent("named"));
      assertEquals(stringEvent("named"), consumer.lastEvent);
    }
  }

  @Test
  void unknownObjectKeyAndOperationReturnDeterministicSystemExceptions() {
    try (NetworkEventService service =
            NetworkEventService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
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
  void invalidRequestBodiesAndDisconnectedProxiesMapToBadParamReplies() {
    NetworkEventServiceClient client = NetworkEventServiceClient.create(IiopOptions.defaults());

    try (NetworkEventService service =
            NetworkEventService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        IiopClient rawClient = IiopClient.connect(service.endpoint(), IiopOptions.defaults())) {
      var supplierAdmin = client.forSuppliers(service.ior());
      var pushConsumer = client.obtainPushConsumer(supplierAdmin);
      byte[] pushKey = IiopObjectReference.fromIor(pushConsumer).objectKey();

      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(3, service.objectReference().objectKey(), "for_suppliers", new byte[] {1})));
      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(
                  4, pushKey, "push", CdrWriter.bigEndian().writeUnsignedLong(99L).toByteArray())));

      client.disconnectPushConsumer(pushConsumer);
      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(
                  5,
                  pushKey,
                  "push",
                  EventServiceIiopCodec.INSTANCE.encodeArguments(
                      EventServiceDescriptors.PUSH, List.of(stringEvent("after-disconnect"))))));
    }
  }

  @Test
  void codecRoundTripsSupportedPrimitiveAnyPayloads() {
    List<AnyValue<?>> values =
        List.of(
            new AnyValue<>(IdlTypeCode.BOOLEAN, true),
            new AnyValue<>(IdlTypeCode.OCTET, 255),
            new AnyValue<>(IdlTypeCode.CHAR, 'A'),
            new AnyValue<>(IdlTypeCode.SHORT, (short) -7),
            new AnyValue<>(IdlTypeCode.UNSIGNED_SHORT, 65_535),
            new AnyValue<>(IdlTypeCode.LONG, -12),
            new AnyValue<>(IdlTypeCode.UNSIGNED_LONG, 0xFFFF_FFFFL),
            new AnyValue<>(IdlTypeCode.LONG_LONG, Long.MIN_VALUE),
            new AnyValue<>(IdlTypeCode.UNSIGNED_LONG_LONG, BigInteger.ONE.shiftLeft(63)),
            new AnyValue<>(IdlTypeCode.FLOAT, 1.25F),
            new AnyValue<>(IdlTypeCode.DOUBLE, -2.5D),
            new AnyValue<>(IdlTypeCode.LONG_DOUBLE, new byte[16]),
            stringEvent("wire"));

    for (AnyValue<?> value : values) {
      byte[] request =
          EventServiceIiopCodec.INSTANCE.encodeArguments(
              EventServiceDescriptors.PUSH, List.of(value));
      assertAnyEquals(
          value,
          (AnyValue<?>)
              EventServiceIiopCodec.INSTANCE
                  .decodeArguments(EventServiceDescriptors.PUSH, request)
                  .get(0));
      assertAnyEquals(
          value,
          (AnyValue<?>)
              EventServiceIiopCodec.INSTANCE.decodeReturnValue(
                  EventServiceDescriptors.PULL,
                  EventServiceIiopCodec.INSTANCE.encodeReturnValue(
                      EventServiceDescriptors.PULL, value)));
      EventTryPullResult result =
          (EventTryPullResult)
              EventServiceIiopCodec.INSTANCE.decodeReturnValue(
                  EventServiceDescriptors.TRY_PULL,
                  EventServiceIiopCodec.INSTANCE.encodeReturnValue(
                      EventServiceDescriptors.TRY_PULL, EventTryPullResult.present(value)));
      assertAnyEquals(value, result.event().orElseThrow());
    }
  }

  @Test
  void tryPullReplyUsesAnyReturnThenBooleanOutParameter() {
    byte[] reply =
        EventServiceIiopCodec.INSTANCE.encodeReturnValue(
            EventServiceDescriptors.TRY_PULL, EventTryPullResult.present(stringEvent("standard")));
    CdrReader reader = CdrReader.bigEndian(reply);

    assertEquals("standard", new AnyWireCodec().read(reader).value());
    assertTrue(reader.readBoolean());
    assertEquals(0, reader.remaining());
  }

  @Test
  void tryPullResultRejectsInconsistentShapes() {
    assertEquals(EventTryPullResult.empty(), new EventTryPullResult(false, Optional.empty()));
    assertEquals(
        EventServiceDiagnosticCodes.NO_EVENT_AVAILABLE,
        assertThrows(
                EventServiceException.class, () -> new EventTryPullResult(true, Optional.empty()))
            .code());
    assertEquals(
        EventServiceDiagnosticCodes.INVALID_PAYLOAD,
        assertThrows(
                EventServiceException.class,
                () -> new EventTryPullResult(false, Optional.of(stringEvent("extra"))))
            .code());
  }

  @Test
  void oldProxyIorAfterChannelDestroyMapsToDeterministicBadParam() {
    NetworkEventServiceClient client = NetworkEventServiceClient.create(IiopOptions.defaults());

    try (NetworkEventService service =
            NetworkEventService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        IiopClient rawClient = IiopClient.connect(service.endpoint(), IiopOptions.defaults())) {
      var supplierAdmin = client.forSuppliers(service.ior());
      var pushConsumer = client.obtainPushConsumer(supplierAdmin);
      byte[] pushKey = IiopObjectReference.fromIor(pushConsumer).objectKey();

      client.destroy(service.ior());

      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(
                  6,
                  pushKey,
                  "push",
                  EventServiceIiopCodec.INSTANCE.encodeArguments(
                      EventServiceDescriptors.PUSH, List.of(stringEvent("stale"))))));
    }
  }

  @Test
  void clientValidatesUnsupportedAnyPayloadsBeforeEncoding() {
    NetworkEventServiceClient client = NetworkEventServiceClient.create(IiopOptions.defaults());

    try (NetworkEventService service =
        NetworkEventService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults())) {
      var supplierAdmin = client.forSuppliers(service.ior());
      var pushConsumer = client.obtainPushConsumer(supplierAdmin);
      AnyValue<List<Integer>> unsupported =
          new AnyValue<>(
              IdlTypeCode.sequenceOf(IdlTypeCode.LONG, "sequence<long>", "java.util.List"),
              List.of(1, 2));

      assertThrows(org.omg.CORBA.BAD_PARAM.class, () -> client.push(pushConsumer, unsupported));
    }
  }

  @Test
  void closeShutsDownLoopbackServer() {
    NetworkEventService service =
        NetworkEventService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
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

  private static AnyValue<String> stringEvent(String value) {
    return new AnyValue<>(IdlTypeCode.STRING, value);
  }

  private static void assertAnyEquals(AnyValue<?> expected, AnyValue<?> actual) {
    assertEquals(expected.typeCode(), actual.typeCode());
    if (expected.value() instanceof byte[] expectedBytes
        && actual.value() instanceof byte[] actualBytes) {
      assertArrayEquals(expectedBytes, actualBytes);
    } else {
      assertEquals(expected.value(), actual.value());
    }
  }

  private static final class RecordingPushConsumer implements EventPushConsumer {
    private AnyValue<?> lastEvent;

    @Override
    public void push(AnyValue<?> event) {
      lastEvent = event;
    }

    @Override
    public void disconnectPushConsumer() {}
  }

  private static final class QueuePullSupplier implements EventPullSupplier {
    private final ArrayDeque<AnyValue<?>> events = new ArrayDeque<>();

    private QueuePullSupplier(AnyValue<?> event) {
      events.add(event);
    }

    @Override
    public Optional<AnyValue<?>> pull() {
      return Optional.ofNullable(events.poll());
    }

    @Override
    public Optional<AnyValue<?>> tryPull() {
      return Optional.ofNullable(events.poll());
    }

    @Override
    public void disconnectPullSupplier() {}
  }
}
