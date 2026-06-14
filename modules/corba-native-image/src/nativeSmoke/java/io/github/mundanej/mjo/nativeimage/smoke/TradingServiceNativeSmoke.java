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
import io.github.mundanej.mjo.trading.LocalTradingOfferRepository;
import io.github.mundanej.mjo.trading.LocalTradingTypeRepository;
import io.github.mundanej.mjo.trading.NetworkTradingService;
import io.github.mundanej.mjo.trading.NetworkTradingServiceClient;
import io.github.mundanej.mjo.trading.TradingImportExportDirection;
import io.github.mundanej.mjo.trading.TradingImportExportLink;
import io.github.mundanej.mjo.trading.TradingOffer;
import io.github.mundanej.mjo.trading.TradingPropertyDefinition;
import io.github.mundanej.mjo.trading.TradingServiceDiagnosticCodes;
import io.github.mundanej.mjo.trading.TradingServiceException;
import io.github.mundanej.mjo.trading.TradingServiceType;
import java.util.List;
import java.util.Map;

/** Native Image smoke coverage for the supported local Trading Service slice. */
public final class TradingServiceNativeSmoke {

  private TradingServiceNativeSmoke() {}

  /** Runs the Trading Service Native Image smoke checks. */
  public static void main(String[] args) {
    TradingServiceType type = orderType();
    TradingOffer offer = orderOffer("offer-a", "MJO", 42L);

    LocalTradingTypeRepository types = new LocalTradingTypeRepository();
    types.register(type);
    LocalTradingOfferRepository offers = new LocalTradingOfferRepository(types);
    offers.register(offer);
    SmokeAssertions.requireEquals(
        List.of(offer), offers.query("OrderService", "symbol == 'MJO'"), "local query");
    assertDiagnostic(
        TradingServiceDiagnosticCodes.UNSUPPORTED_CONSTRAINT_OPERATOR,
        () -> offers.query("OrderService", "matches(symbol) == true"),
        "unsupported constraint rejection");
    offers.registerImportExportLink(
        new TradingImportExportLink("import-a", TradingImportExportDirection.IMPORT, "peer-a"));
    assertDiagnostic(
        TradingServiceDiagnosticCodes.REMOTE_FEDERATION_DISABLED,
        () -> offers.rejectRemoteImportQuery("import-a"),
        "remote import disabled diagnostic");

    try (NetworkTradingService service =
            NetworkTradingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkTradingServiceClient client =
            NetworkTradingServiceClient.connect(service.ior(), IiopOptions.defaults())) {
      client.registerType(type);
      client.registerOffer(offer);
      SmokeAssertions.requireEquals(
          List.of(offer), client.queryOffers("OrderService", "quantity >= 40"), "IIOP query");
      client.registerImportLink("import-a", "peer-a");
      SmokeAssertions.requireEquals(
          List.of(
              new TradingImportExportLink(
                  "import-a", TradingImportExportDirection.IMPORT, "peer-a")),
          client.listImportExportLinks(),
          "IIOP import metadata");
      SmokeAssertions.requireEquals(offer, client.withdrawOffer("offer-a"), "IIOP withdraw");
    }

    try (NetworkNamingService naming =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkNamingClient namingClient =
            NetworkNamingClient.connect(naming.ior(), IiopOptions.defaults());
        NetworkTradingService service =
            NetworkTradingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults())) {
      service.bindInNaming(namingClient, NamingName.parse("TradingService"));
      RemoteNamingBindingTarget target = namingClient.resolve(NamingName.parse("TradingService"));
      try (NetworkTradingServiceClient client =
          NetworkTradingServiceClient.connect(target.ior(), IiopOptions.defaults())) {
        client.registerType(type);
        client.registerOffer(orderOffer("offer-b", "MJO", 7L));
        SmokeAssertions.requireEquals(
            List.of(orderOffer("offer-b", "MJO", 7L)),
            client.queryOffers("OrderService", "symbol == 'MJO'"),
            "Naming-resolved query");
      }
    }

    NetworkTradingService closed =
        NetworkTradingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
    IiopEndpoint endpoint = closed.endpoint();
    closed.close();
    SmokeAssertions.requireThrows(
        IiopException.class,
        () -> IiopClient.connect(endpoint, IiopOptions.defaults()),
        "Trading Service close shuts down server");
  }

  private static TradingServiceType orderType() {
    return new TradingServiceType(
        "OrderService",
        List.of(
            TradingPropertyDefinition.requiredString("symbol"),
            TradingPropertyDefinition.requiredSignedLong("quantity")));
  }

  private static TradingOffer orderOffer(String id, String symbol, long quantity) {
    return new TradingOffer(
        id, "OrderService", Map.of("symbol", symbol, "quantity", Long.valueOf(quantity)));
  }

  private static void assertDiagnostic(
      DiagnosticCode code, SmokeAssertions.ThrowingAction action, String label) {
    try {
      action.run();
    } catch (TradingServiceException expected) {
      SmokeAssertions.requireEquals(code, expected.code(), label);
      return;
    } catch (Exception exception) {
      throw new AssertionError("Native Image smoke failed: " + label, exception);
    }
    throw new AssertionError("Native Image smoke failed: " + label + "; expected diagnostic");
  }
}
