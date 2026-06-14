package io.github.mundanej.mjo.trading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class LocalTradingImportExportBoundaryTest {

  @Test
  void registersAndListsBoundedLinksDeterministically() {
    LocalTradingImportExportBoundary boundary = new LocalTradingImportExportBoundary();

    TradingImportExportLink importLink =
        boundary.register(
            new TradingImportExportLink(
                "import-alpha", TradingImportExportDirection.IMPORT, "PeerTraderA"));
    TradingImportExportLink exportLink =
        boundary.register(
            new TradingImportExportLink(
                "export-beta", TradingImportExportDirection.EXPORT, "PeerTraderB"));

    assertEquals(List.of(importLink, exportLink), boundary.list());
    assertEquals(List.of(importLink), boundary.list(TradingImportExportDirection.IMPORT));
    assertEquals(List.of(exportLink), boundary.list(TradingImportExportDirection.EXPORT));
    assertEquals(importLink, boundary.lookup("import-alpha").orElseThrow());
  }

  @Test
  void rejectsDuplicateMissingMalformedAndFanOutLimitFailures() {
    LocalTradingImportExportBoundary boundary =
        new LocalTradingImportExportBoundary(new TradingImportExportOptions(1, 12, 12));
    boundary.register(
        new TradingImportExportLink("import-a", TradingImportExportDirection.IMPORT, "peer-a"));

    TradingServiceException duplicate =
        assertThrows(
            TradingServiceException.class,
            () ->
                boundary.register(
                    new TradingImportExportLink(
                        "import-a", TradingImportExportDirection.EXPORT, "peer-b")));
    TradingServiceException limit =
        assertThrows(
            TradingServiceException.class,
            () ->
                boundary.register(
                    new TradingImportExportLink(
                        "export-b", TradingImportExportDirection.EXPORT, "peer-b")));
    TradingServiceException missing =
        assertThrows(TradingServiceException.class, () -> boundary.remove("missing"));
    TradingServiceException malformed =
        assertThrows(
            TradingServiceException.class,
            () -> new TradingImportExportLink("bad", null, "peer-c"));
    TradingServiceException oversized =
        assertThrows(
            TradingServiceException.class,
            () ->
                boundary.register(
                    new TradingImportExportLink(
                        "import-name-too-long", TradingImportExportDirection.IMPORT, "peer-c")));

    assertEquals(TradingServiceDiagnosticCodes.LINK_ALREADY_EXISTS, duplicate.code());
    assertEquals(TradingServiceDiagnosticCodes.LINK_LIMIT_EXCEEDED, limit.code());
    assertEquals(TradingServiceDiagnosticCodes.LINK_NOT_FOUND, missing.code());
    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_LINK, malformed.code());
    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_NAME, oversized.code());
  }

  @Test
  void rejectsRemoteFederationDeterministically() {
    LocalTradingImportExportBoundary boundary = new LocalTradingImportExportBoundary();
    boundary.register(
        new TradingImportExportLink("import-a", TradingImportExportDirection.IMPORT, "peer-a"));
    boundary.register(
        new TradingImportExportLink("export-a", TradingImportExportDirection.EXPORT, "peer-b"));

    TradingServiceException disabled =
        assertThrows(TradingServiceException.class, () -> boundary.rejectRemoteQuery("import-a"));
    TradingServiceException wrongDirection =
        assertThrows(TradingServiceException.class, () -> boundary.rejectRemoteQuery("export-a"));
    TradingServiceException missing =
        assertThrows(TradingServiceException.class, () -> boundary.rejectRemoteQuery("missing"));

    assertEquals(TradingServiceDiagnosticCodes.REMOTE_FEDERATION_DISABLED, disabled.code());
    assertEquals(TradingServiceDiagnosticCodes.LINK_DIRECTION_MISMATCH, wrongDirection.code());
    assertEquals(TradingServiceDiagnosticCodes.LINK_NOT_FOUND, missing.code());
  }

  @Test
  void keepsLocalQueryIsolatedFromImportExportMetadata() {
    LocalTradingTypeRepository types = new LocalTradingTypeRepository();
    types.register(
        new TradingServiceType(
            "OrderService", List.of(TradingPropertyDefinition.requiredString("symbol"))));
    LocalTradingOfferRepository repository =
        new LocalTradingOfferRepository(types, TradingOfferRepositoryOptions.defaults());
    TradingImportExportLink importLink =
        repository.registerImportExportLink(
            new TradingImportExportLink("import-a", TradingImportExportDirection.IMPORT, "peer-a"));
    repository.register(new TradingOffer("offer-a", "OrderService", Map.of("symbol", "MJO")));

    List<TradingOffer> matches = repository.query("OrderService", "symbol == 'MJO'");

    assertEquals(List.of(importLink), repository.listImportExportLinks());
    assertEquals(List.of("offer-a"), matches.stream().map(TradingOffer::id).toList());
    assertTrue(repository.query("OrderService", "symbol == 'missing'").isEmpty());
  }
}
