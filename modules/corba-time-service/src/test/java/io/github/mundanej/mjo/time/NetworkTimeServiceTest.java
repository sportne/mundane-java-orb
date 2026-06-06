package io.github.mundanej.mjo.time;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.naming.server.NetworkNamingClient;
import io.github.mundanej.mjo.naming.server.NetworkNamingService;
import io.github.mundanej.mjo.naming.server.RemoteNamingBindingTarget;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Loopback IIOP and Naming tests for the supported Time Service subset. */
@Tag("unit")
final class NetworkTimeServiceTest {

  @Test
  void universalTimeNewUniversalTimeAndNewIntervalDispatchOverIiop() {
    LocalTimeService local =
        LocalTimeService.create(
            new TimeServiceOptions(
                Clock.fixed(Instant.parse("1582-10-15T00:00:03.000000101Z"), ZoneOffset.UTC),
                Duration.ofNanos(101),
                ZoneOffset.ofHours(-5)));

    try (NetworkTimeService service =
            NetworkTimeService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), local);
        NetworkTimeServiceClient client =
            NetworkTimeServiceClient.connect(service.objectReference(), IiopOptions.defaults())) {
      assertEquals(new UtcTime(30_000_001L, 2L, (short) -300), client.universalTime());
      assertEquals(
          new UtcTime(10L, (1L << 40) + 5L, (short) 60),
          client.newUniversalTime(10L, (1L << 40) + 5L, (short) 60));
      assertEquals(new TimeInterval(7L, 12L), client.newInterval(7L, 12L));

      assertEquals("IDL:omg.org/CosTime/TimeService:1.0", service.objectReference().ior().typeId());
      assertArrayEquals(
          NetworkTimeService.DEFAULT_OBJECT_ID.getBytes(StandardCharsets.US_ASCII),
          service.objectReference().objectKey());
    }
  }

  @Test
  void timeServiceIorCanBeBoundAndResolvedThroughNetworkNaming() {
    LocalTimeService local =
        LocalTimeService.create(
            new TimeServiceOptions(
                Clock.fixed(Instant.parse("1582-10-15T00:00:01Z"), ZoneOffset.UTC),
                Duration.ZERO,
                ZoneOffset.UTC));

    try (NetworkNamingService naming =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkNamingClient namingClient =
            NetworkNamingClient.connect(naming.ior(), IiopOptions.defaults());
        NetworkTimeService service =
            NetworkTimeService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), local)) {
      service.bindInNaming(namingClient, NamingName.parse("TimeService"));
      RemoteNamingBindingTarget target = namingClient.resolve(NamingName.parse("TimeService"));

      assertEquals(RemoteNamingBindingTarget.Kind.OBJECT, target.kind());
      try (NetworkTimeServiceClient timeClient =
          NetworkTimeServiceClient.connect(target.ior(), IiopOptions.defaults())) {
        assertEquals(new UtcTime(10_000_000L, 0L, (short) 0), timeClient.universalTime());
      }
    }
  }

  @Test
  void unknownObjectKeyAndOperationReturnDeterministicSystemExceptions() {
    LocalTimeService local = LocalTimeService.systemUtc();
    try (NetworkTimeService service =
            NetworkTimeService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), local);
        IiopClient rawClient = IiopClient.connect(service.endpoint(), IiopOptions.defaults())) {
      assertSystemException(
          "IDL:omg.org/CORBA/OBJECT_NOT_EXIST:1.0",
          rawClient.invoke(
              request(
                  1,
                  "missing".getBytes(StandardCharsets.US_ASCII),
                  "universal_time",
                  new byte[0])));
      assertSystemException(
          "IDL:omg.org/CORBA/BAD_OPERATION:1.0",
          rawClient.invoke(
              request(2, service.objectReference().objectKey(), "missing", new byte[0])));
    }
  }

  @Test
  void invalidRequestBodiesMapToBadParamReplies() {
    LocalTimeService local = LocalTimeService.systemUtc();
    try (NetworkTimeService service =
            NetworkTimeService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), local);
        IiopClient rawClient = IiopClient.connect(service.endpoint(), IiopOptions.defaults())) {
      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(
                  3,
                  service.objectReference().objectKey(),
                  "new_interval",
                  CdrWriter.bigEndian()
                      .writeUnsignedLongLong(BigInteger.valueOf(9L))
                      .toByteArray())));
      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(
                  4,
                  service.objectReference().objectKey(),
                  "new_universal_time",
                  CdrWriter.bigEndian()
                      .writeUnsignedLongLong(BigInteger.ONE.shiftLeft(63))
                      .writeUnsignedLong(0)
                      .writeUnsignedShort(0)
                      .writeShort((short) 0)
                      .toByteArray())));
      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(
                  5,
                  service.objectReference().objectKey(),
                  "new_interval",
                  CdrWriter.bigEndian()
                      .writeUnsignedLongLong(BigInteger.valueOf(9L))
                      .writeUnsignedLongLong(BigInteger.valueOf(2L))
                      .toByteArray())));
    }
  }

  @Test
  void clientValidatesInvalidValueInputsBeforeEncoding() {
    LocalTimeService local = LocalTimeService.systemUtc();
    try (NetworkTimeService service =
            NetworkTimeService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), local);
        NetworkTimeServiceClient client =
            NetworkTimeServiceClient.connect(service.objectReference(), IiopOptions.defaults())) {
      assertEquals(
          TimeServiceDiagnosticCodes.INVALID_INACCURACY,
          assertThrows(
                  TimeServiceException.class, () -> client.newUniversalTime(1L, -1L, (short) 0))
              .code());
      assertEquals(
          TimeServiceDiagnosticCodes.INVALID_INTERVAL,
          assertThrows(TimeServiceException.class, () -> client.newInterval(9L, 2L)).code());
    }
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
}
