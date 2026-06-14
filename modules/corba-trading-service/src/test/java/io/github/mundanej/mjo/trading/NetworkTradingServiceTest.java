package io.github.mundanej.mjo.trading;

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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Loopback IIOP and Naming tests for the supported Trading Service subset. */
@Tag("unit")
final class NetworkTradingServiceTest {

  @Test
  void typeOfferAndQueryOperationsDispatchOverIiop() {
    try (NetworkTradingService service =
            NetworkTradingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkTradingServiceClient client =
            NetworkTradingServiceClient.connect(
                service.objectReference(), IiopOptions.defaults())) {
      client.registerType(orderType());
      client.updateType(updatedOrderType());
      client.registerOffer(orderOffer("offer-b", "MJO", 7L));
      client.registerOffer(orderOffer("offer-a", "MJO", 10L));
      client.registerOffer(orderOffer("offer-c", "HAL", 1L));

      assertEquals(updatedOrderType(), client.lookupType("OrderService"));
      assertEquals(List.of(updatedOrderType()), client.listTypes());
      assertEquals(
          List.of("offer-a", "offer-b"),
          client.queryOffers("OrderService", "symbol == 'MJO' and priority > 5").stream()
              .map(TradingOffer::id)
              .toList());
      assertEquals("offer-b", client.withdrawOffer("offer-b").id());
      assertEquals(
          List.of("offer-a"),
          client.queryOffers("OrderService", "symbol == 'MJO'").stream()
              .map(TradingOffer::id)
              .toList());

      assertEquals("IDL:omg.org/CosTrading/Lookup:1.0", service.objectReference().ior().typeId());
      assertArrayEquals(
          NetworkTradingService.DEFAULT_OBJECT_ID.getBytes(StandardCharsets.US_ASCII),
          service.objectReference().objectKey());
    }
  }

  @Test
  void traderIorCanBeBoundAndResolvedThroughNetworkNaming() {
    try (NetworkNamingService naming =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkNamingClient namingClient =
            NetworkNamingClient.connect(naming.ior(), IiopOptions.defaults());
        NetworkTradingService service =
            NetworkTradingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults())) {
      service.bindInNaming(namingClient, NamingName.parse("Trader"));
      RemoteNamingBindingTarget target = namingClient.resolve(NamingName.parse("Trader"));

      assertEquals(RemoteNamingBindingTarget.Kind.OBJECT, target.kind());
      try (NetworkTradingServiceClient tradingClient =
          NetworkTradingServiceClient.connect(target.ior(), IiopOptions.defaults())) {
        tradingClient.registerType(orderType());
        assertEquals(orderType(), tradingClient.lookupType("OrderService"));
      }
    }
  }

  @Test
  void unknownObjectKeyAndOperationReturnDeterministicSystemExceptions() {
    try (NetworkTradingService service =
            NetworkTradingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        IiopClient rawClient = IiopClient.connect(service.endpoint(), IiopOptions.defaults())) {
      assertSystemException(
          "IDL:omg.org/CORBA/OBJECT_NOT_EXIST:1.0",
          rawClient.invoke(
              request(
                  1,
                  "stale-trader".getBytes(StandardCharsets.US_ASCII),
                  "list_types",
                  new byte[0])));
      assertSystemException(
          "IDL:omg.org/CORBA/BAD_OPERATION:1.0",
          rawClient.invoke(
              request(2, service.objectReference().objectKey(), "missing", new byte[0])));
    }
  }

  @Test
  void malformedBodiesAndDisabledImportReturnBadParamReplies() {
    try (NetworkTradingService service =
            NetworkTradingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkTradingServiceClient client =
            NetworkTradingServiceClient.connect(service.objectReference(), IiopOptions.defaults());
        IiopClient rawClient = IiopClient.connect(service.endpoint(), IiopOptions.defaults())) {
      client.registerImportLink("import-a", "peer-a");

      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(
                  3,
                  service.objectReference().objectKey(),
                  "query_offers",
                  CdrWriter.bigEndian().writeString("OrderService").toByteArray())));
      assertSystemException(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          rawClient.invoke(
              request(
                  4,
                  service.objectReference().objectKey(),
                  "reject_remote_import_query",
                  CdrWriter.bigEndian().writeString("import-a").toByteArray())));
    }
  }

  @Test
  void importExportMetadataDispatchesOverIiop() {
    try (NetworkTradingService service =
            NetworkTradingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkTradingServiceClient client =
            NetworkTradingServiceClient.connect(
                service.objectReference(), IiopOptions.defaults())) {
      client.registerImportLink("import-a", "peer-a");
      client.registerExportLink("export-a", "peer-b");

      assertEquals(
          List.of(
              new TradingImportExportLink(
                  "import-a", TradingImportExportDirection.IMPORT, "peer-a"),
              new TradingImportExportLink(
                  "export-a", TradingImportExportDirection.EXPORT, "peer-b")),
          client.listImportExportLinks());
    }
  }

  @Test
  void closeStopsLoopbackServerCleanly() {
    NetworkTradingService service =
        NetworkTradingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
    IiopEndpoint endpoint = service.endpoint();

    service.close();

    assertThrows(
        io.github.mundanej.mjo.iiop.IiopException.class,
        () -> IiopClient.connect(endpoint, IiopOptions.defaults()));
  }

  private static TradingServiceType orderType() {
    return new TradingServiceType(
        "OrderService",
        List.of(
            TradingPropertyDefinition.requiredString("symbol"),
            TradingPropertyDefinition.requiredSignedLong("priority")));
  }

  private static TradingServiceType updatedOrderType() {
    return new TradingServiceType(
        "OrderService",
        List.of(
            TradingPropertyDefinition.requiredString("symbol"),
            TradingPropertyDefinition.requiredSignedLong("priority"),
            new TradingPropertyDefinition("region", TradingPrimitiveKind.STRING, false)));
  }

  private static TradingOffer orderOffer(String id, String symbol, long priority) {
    return new TradingOffer(
        id, "OrderService", Map.of("symbol", symbol, "priority", Long.valueOf(priority)));
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
