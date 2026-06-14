package io.github.mundanej.mjo.trading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class LocalTradingOfferRepositoryTest {

  @Test
  void registersAndLooksUpOffers() {
    LocalTradingOfferRepository repository = offerRepository();

    TradingOffer registered = repository.register(orderOffer("offer-1", "ACME", true, 7L, 1.5D));

    assertEquals("offer-1", registered.id());
    assertEquals("OrderService", registered.typeName());
    assertEquals("ACME", registered.properties().get("symbol"));
    assertEquals(List.of(registered), repository.listByType("OrderService"));
    assertEquals(registered, repository.lookup("offer-1").orElseThrow());
  }

  @Test
  void preservesListByTypeInsertionOrder() {
    LocalTradingTypeRepository types = typeRepository();
    types.register(TradingServiceType.empty("AuditService"));
    LocalTradingOfferRepository repository = new LocalTradingOfferRepository(types);
    TradingOffer first = repository.register(orderOffer("offer-1", "ACME", true, 7L, 1.5D));
    repository.register(TradingOffer.empty("audit-1", "AuditService"));
    TradingOffer second = repository.register(orderOffer("offer-2", "HAL", false, 8L, 2.5D));

    assertEquals(List.of(first, second), repository.listByType("OrderService"));
  }

  @Test
  void updatesAndWithdrawsExistingOffers() {
    LocalTradingOfferRepository repository = offerRepository();
    repository.register(orderOffer("offer-1", "ACME", true, 7L, 1.5D));

    TradingOffer updated = repository.update(orderOffer("offer-1", "HAL", false, 8L, 2.5D));
    TradingOffer withdrawn = repository.withdraw("offer-1");

    assertEquals(updated, withdrawn);
    assertFalse(repository.lookup("offer-1").isPresent());
    TradingServiceException missingWithdraw =
        assertThrows(TradingServiceException.class, () -> repository.withdraw("offer-1"));
    assertEquals(TradingServiceDiagnosticCodes.OFFER_NOT_FOUND, missingWithdraw.code());
  }

  @Test
  void rejectsMissingTypeDuplicateOfferAndMissingUpdate() {
    LocalTradingOfferRepository repository = offerRepository();
    repository.register(orderOffer("offer-1", "ACME", true, 7L, 1.5D));

    TradingServiceException missingType =
        assertThrows(
            TradingServiceException.class,
            () -> repository.register(TradingOffer.empty("bad", "MissingService")));
    TradingServiceException duplicate =
        assertThrows(
            TradingServiceException.class,
            () -> repository.register(orderOffer("offer-1", "ACME", true, 7L, 1.5D)));
    TradingServiceException missingUpdate =
        assertThrows(
            TradingServiceException.class,
            () -> repository.update(orderOffer("missing", "ACME", true, 7L, 1.5D)));

    assertEquals(TradingServiceDiagnosticCodes.TYPE_NOT_FOUND, missingType.code());
    assertEquals(TradingServiceDiagnosticCodes.OFFER_ALREADY_EXISTS, duplicate.code());
    assertEquals(TradingServiceDiagnosticCodes.OFFER_NOT_FOUND, missingUpdate.code());
  }

  @Test
  void rejectsPropertyMismatches() {
    LocalTradingOfferRepository repository = offerRepository();

    TradingServiceException missingRequired =
        assertThrows(
            TradingServiceException.class,
            () -> repository.register(new TradingOffer("bad-1", "OrderService", Map.of())));
    TradingServiceException unknownProperty =
        assertThrows(
            TradingServiceException.class,
            () ->
                repository.register(
                    new TradingOffer(
                        "bad-2",
                        "OrderService",
                        Map.of(
                            "symbol",
                            "ACME",
                            "active",
                            true,
                            "priority",
                            1L,
                            "ratio",
                            1.0D,
                            "unknown",
                            "value"))));
    TradingServiceException wrongKind =
        assertThrows(
            TradingServiceException.class,
            () -> repository.register(orderOffer("bad-3", "ACME", true, 7L, "not-a-double")));

    assertEquals(TradingServiceDiagnosticCodes.PROPERTY_MISMATCH, missingRequired.code());
    assertEquals(TradingServiceDiagnosticCodes.PROPERTY_MISMATCH, unknownProperty.code());
    assertEquals(TradingServiceDiagnosticCodes.UNSUPPORTED_VALUE, wrongKind.code());
  }

  @Test
  void rejectsUnsupportedValuesAndConfiguredValueLimits() {
    LocalTradingTypeRepository types =
        new LocalTradingTypeRepository(new TradingServiceOptions(2, 8, 16));
    types.register(orderType());
    LocalTradingOfferRepository repository =
        new LocalTradingOfferRepository(types, new TradingOfferRepositoryOptions(2, 4, 8, 4));

    TradingServiceException invalidLimit =
        assertThrows(
            TradingServiceException.class, () -> new TradingOfferRepositoryOptions(0, 1, 1, 1));
    TradingServiceException longId =
        assertThrows(
            TradingServiceException.class,
            () -> repository.register(orderOffer("offer-too-long", "ACME", true, 7L, 1.5D)));
    TradingServiceException longString =
        assertThrows(
            TradingServiceException.class,
            () -> repository.register(orderOffer("offer-1", "ABCDE", true, 7L, 1.5D)));
    TradingServiceException infinite =
        assertThrows(
            TradingServiceException.class,
            () ->
                repository.register(
                    orderOffer("offer-2", "ACME", true, 7L, Double.POSITIVE_INFINITY)));

    assertEquals(TradingServiceDiagnosticCodes.INVALID_LIMIT, invalidLimit.code());
    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_NAME, longId.code());
    assertEquals(TradingServiceDiagnosticCodes.VALUE_LIMIT_EXCEEDED, longString.code());
    assertEquals(TradingServiceDiagnosticCodes.UNSUPPORTED_VALUE, infinite.code());
  }

  @Test
  void enforcesOfferAndPropertyCountLimits() {
    LocalTradingOfferRepository repository =
        new LocalTradingOfferRepository(
            typeRepository(), new TradingOfferRepositoryOptions(1, 3, 16, 16));

    TradingServiceException propertyLimit =
        assertThrows(
            TradingServiceException.class,
            () -> repository.register(orderOffer("bad", "ACME", true, 7L, 1.5D)));
    LocalTradingOfferRepository oneOfferRepository =
        new LocalTradingOfferRepository(
            typeRepository(), new TradingOfferRepositoryOptions(1, 4, 16, 16));
    oneOfferRepository.register(orderOffer("offer-1", "ACME", true, 7L, 1.5D));
    TradingServiceException offerLimit =
        assertThrows(
            TradingServiceException.class,
            () -> oneOfferRepository.register(orderOffer("offer-2", "HAL", true, 8L, 2.5D)));

    assertEquals(TradingServiceDiagnosticCodes.PROPERTY_LIMIT_EXCEEDED, propertyLimit.code());
    assertEquals(TradingServiceDiagnosticCodes.OFFER_LIMIT_EXCEEDED, offerLimit.code());
  }

  @Test
  void snapshotsAreImmutableAndIndependentOfCallerMaps() {
    LocalTradingOfferRepository repository = offerRepository();
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("symbol", "ACME");
    properties.put("active", true);
    properties.put("priority", 7L);
    properties.put("ratio", 1.5D);

    TradingOffer registered =
        repository.register(new TradingOffer("offer-1", "OrderService", properties));
    properties.put("symbol", "HAL");

    assertEquals("ACME", registered.properties().get("symbol"));
    assertThrows(
        UnsupportedOperationException.class, () -> registered.properties().put("symbol", "HAL"));
    assertThrows(
        UnsupportedOperationException.class,
        () -> repository.listByType("OrderService").add(registered));
  }

  @Test
  void lookupAndListValidateIdentifiersButMissingOfferIsOptionalEmpty() {
    LocalTradingOfferRepository repository = offerRepository();

    assertTrue(repository.lookup("missing").isEmpty());
    TradingServiceException malformedOfferId =
        assertThrows(TradingServiceException.class, () -> repository.lookup(""));
    TradingServiceException missingType =
        assertThrows(TradingServiceException.class, () -> repository.listByType("MissingService"));

    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_NAME, malformedOfferId.code());
    assertEquals(TradingServiceDiagnosticCodes.TYPE_NOT_FOUND, missingType.code());
  }

  private static LocalTradingOfferRepository offerRepository() {
    return new LocalTradingOfferRepository(typeRepository());
  }

  private static LocalTradingTypeRepository typeRepository() {
    LocalTradingTypeRepository repository = new LocalTradingTypeRepository();
    repository.register(orderType());
    return repository;
  }

  private static TradingServiceType orderType() {
    return new TradingServiceType(
        "OrderService",
        List.of(
            TradingPropertyDefinition.requiredString("symbol"),
            TradingPropertyDefinition.requiredBoolean("active"),
            TradingPropertyDefinition.requiredSignedLong("priority"),
            TradingPropertyDefinition.requiredFloatingPoint("ratio")));
  }

  private static TradingOffer orderOffer(
      String id, Object symbol, Object active, Object priority, Object ratio) {
    return new TradingOffer(
        id,
        "OrderService",
        Map.of("symbol", symbol, "active", active, "priority", priority, "ratio", ratio));
  }
}
