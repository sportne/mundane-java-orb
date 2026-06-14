package io.github.mundanej.mjo.trading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class LocalTradingQueryTest {

  @Test
  void queriesExactAndComparisonMatches() {
    LocalTradingOfferRepository repository = populatedRepository();

    List<TradingOffer> exact = repository.query("OrderService", "symbol == 'MJO'");
    List<TradingOffer> comparison =
        repository.query("OrderService", "priority > 5 and ratio >= 1.0");

    assertEquals(List.of("offer-a", "offer-b"), offerIds(exact));
    assertEquals(List.of("offer-a", "offer-b"), offerIds(comparison));
  }

  @Test
  void returnsEmptyResultsForValidNonMatches() {
    LocalTradingOfferRepository repository = populatedRepository();

    List<TradingOffer> matches = repository.query("OrderService", "symbol == 'NONE'");

    assertTrue(matches.isEmpty());
  }

  @Test
  void returnsDeterministicOfferIdOrdering() {
    LocalTradingOfferRepository repository = populatedRepository();

    List<TradingOffer> matches = repository.query("OrderService", "true");

    assertEquals(List.of("offer-a", "offer-b", "offer-c"), offerIds(matches));
  }

  @Test
  void rejectsMaxResultAndQueryCostLimitViolations() {
    LocalTradingOfferRepository repository = populatedRepository();

    TradingServiceException resultLimit =
        assertThrows(
            TradingServiceException.class,
            () -> repository.query("OrderService", "true", new TradingQueryOptions(2, 8)));
    TradingServiceException costLimit =
        assertThrows(
            TradingServiceException.class,
            () -> repository.query("OrderService", "true", new TradingQueryOptions(8, 2)));
    TradingServiceException invalidLimit =
        assertThrows(TradingServiceException.class, () -> new TradingQueryOptions(0, 1));

    assertEquals(TradingServiceDiagnosticCodes.QUERY_LIMIT_EXCEEDED, resultLimit.code());
    assertEquals(TradingServiceDiagnosticCodes.QUERY_LIMIT_EXCEEDED, costLimit.code());
    assertEquals(TradingServiceDiagnosticCodes.INVALID_LIMIT, invalidLimit.code());
  }

  @Test
  void preservesClearUnknownTypeAndConstraintDiagnostics() {
    LocalTradingOfferRepository repository = populatedRepository();

    TradingServiceException unknownType =
        assertThrows(
            TradingServiceException.class, () -> repository.query("MissingService", "true"));
    TradingServiceException malformed =
        assertThrows(
            TradingServiceException.class, () -> repository.query("OrderService", "symbol =="));
    TradingServiceException unknownProperty =
        assertThrows(
            TradingServiceException.class,
            () -> repository.query("OrderService", "missing == 'value'"));
    TradingServiceException typeMismatch =
        assertThrows(
            TradingServiceException.class,
            () -> repository.query("OrderService", "priority == '10'"));

    assertEquals(TradingServiceDiagnosticCodes.TYPE_NOT_FOUND, unknownType.code());
    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_CONSTRAINT, malformed.code());
    assertEquals(TradingServiceDiagnosticCodes.UNKNOWN_CONSTRAINT_PROPERTY, unknownProperty.code());
    assertEquals(TradingServiceDiagnosticCodes.CONSTRAINT_TYPE_MISMATCH, typeMismatch.code());
  }

  @Test
  void validatesConstraintPropertiesAgainstRegisteredType() {
    LocalTradingTypeRepository types = new LocalTradingTypeRepository();
    types.register(orderType());
    LocalTradingOfferRepository repository = new LocalTradingOfferRepository(types);

    TradingServiceException unknownProperty =
        assertThrows(
            TradingServiceException.class,
            () -> repository.query("OrderService", "missing == 'value'"));
    TradingServiceException typeMismatch =
        assertThrows(
            TradingServiceException.class,
            () -> repository.query("OrderService", "priority == '10'"));

    assertEquals(TradingServiceDiagnosticCodes.UNKNOWN_CONSTRAINT_PROPERTY, unknownProperty.code());
    assertEquals(TradingServiceDiagnosticCodes.CONSTRAINT_TYPE_MISMATCH, typeMismatch.code());
  }

  @Test
  void treatsOmittedOptionalPropertiesAsNonMatches() {
    LocalTradingTypeRepository types = new LocalTradingTypeRepository();
    types.register(
        new TradingServiceType(
            "RegionalOrderService",
            List.of(
                TradingPropertyDefinition.requiredString("symbol"),
                new TradingPropertyDefinition("region", TradingPrimitiveKind.STRING, false))));
    LocalTradingOfferRepository repository = new LocalTradingOfferRepository(types);
    repository.register(
        new TradingOffer("offer-without-region", "RegionalOrderService", Map.of("symbol", "MJO")));
    repository.register(
        new TradingOffer(
            "offer-with-region", "RegionalOrderService", Map.of("symbol", "MJO", "region", "US")));

    List<TradingOffer> matches = repository.query("RegionalOrderService", "region == 'US'");

    assertEquals(List.of("offer-with-region"), offerIds(matches));
  }

  @Test
  void keepsQueriesTypeScoped() {
    LocalTradingOfferRepository repository = populatedRepository();

    List<TradingOffer> auditMatches = repository.query("AuditService", "true");

    assertEquals(List.of("audit-a"), offerIds(auditMatches));
  }

  private static LocalTradingOfferRepository populatedRepository() {
    LocalTradingTypeRepository types = new LocalTradingTypeRepository();
    types.register(orderType());
    types.register(TradingServiceType.empty("AuditService"));
    LocalTradingOfferRepository repository = new LocalTradingOfferRepository(types);
    repository.register(orderOffer("offer-b", "MJO", true, 7L, 1.0D));
    repository.register(orderOffer("offer-a", "MJO", true, 10L, 2.0D));
    repository.register(orderOffer("offer-c", "HAL", false, 1L, 0.5D));
    repository.register(TradingOffer.empty("audit-a", "AuditService"));
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

  private static List<String> offerIds(List<TradingOffer> offers) {
    return offers.stream().map(TradingOffer::id).toList();
  }
}
