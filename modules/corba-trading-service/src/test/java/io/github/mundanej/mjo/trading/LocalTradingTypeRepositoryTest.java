package io.github.mundanej.mjo.trading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class LocalTradingTypeRepositoryTest {

  @Test
  void registersAndLooksUpServiceTypes() {
    LocalTradingTypeRepository repository = new LocalTradingTypeRepository();
    TradingServiceType registered = repository.register(orderType());

    assertEquals("OrderService", registered.name());
    assertEquals(TradingPrimitiveKind.STRING, registered.properties().get(0).kind());
    assertEquals(TradingPrimitiveKind.BOOLEAN, registered.properties().get(1).kind());
    assertEquals(TradingPrimitiveKind.SIGNED_LONG, registered.properties().get(2).kind());
    assertEquals(List.of(registered), repository.list());
    assertEquals(registered, repository.lookup("OrderService").orElseThrow());
  }

  @Test
  void rejectsDuplicateTypeRegistrationAndMissingUpdates() {
    LocalTradingTypeRepository repository = new LocalTradingTypeRepository();
    repository.register(TradingServiceType.empty("OrderService"));

    TradingServiceException duplicate =
        assertThrows(
            TradingServiceException.class,
            () -> repository.register(TradingServiceType.empty("OrderService")));
    TradingServiceException missingUpdate =
        assertThrows(
            TradingServiceException.class,
            () -> repository.update(TradingServiceType.empty("MissingService")));

    assertEquals(TradingServiceDiagnosticCodes.TYPE_ALREADY_EXISTS, duplicate.code());
    assertEquals(TradingServiceDiagnosticCodes.TYPE_NOT_FOUND, missingUpdate.code());
  }

  @Test
  void updatesAndDeletesExistingTypes() {
    LocalTradingTypeRepository repository = new LocalTradingTypeRepository();
    repository.register(TradingServiceType.empty("OrderService"));

    TradingServiceType updated = repository.update(orderType());
    TradingServiceType deleted = repository.delete("OrderService");

    assertEquals(updated, deleted);
    assertFalse(repository.lookup("OrderService").isPresent());
    TradingServiceException missingDelete =
        assertThrows(TradingServiceException.class, () -> repository.delete("OrderService"));
    assertEquals(TradingServiceDiagnosticCodes.TYPE_NOT_FOUND, missingDelete.code());
  }

  @Test
  void enforcesConfiguredTypeAndPropertyLimits() {
    LocalTradingTypeRepository repository =
        new LocalTradingTypeRepository(new TradingServiceOptions(1, 1, 16));
    repository.register(
        new TradingServiceType("One", List.of(TradingPropertyDefinition.requiredString("name"))));

    TradingServiceException typeLimit =
        assertThrows(
            TradingServiceException.class,
            () -> repository.register(TradingServiceType.empty("Two")));
    TradingServiceException propertyLimit =
        assertThrows(
            TradingServiceException.class, () -> repository.update(twoPropertyType("One")));

    assertEquals(TradingServiceDiagnosticCodes.TYPE_LIMIT_EXCEEDED, typeLimit.code());
    assertEquals(TradingServiceDiagnosticCodes.PROPERTY_LIMIT_EXCEEDED, propertyLimit.code());
  }

  @Test
  void supportsCallerConfiguredLimitsAboveDefaults() {
    LocalTradingTypeRepository repository =
        new LocalTradingTypeRepository(new TradingServiceOptions(2, 65, 130));
    List<TradingPropertyDefinition> definitions = new ArrayList<>();
    for (int index = 0; index < 65; index++) {
      definitions.add(TradingPropertyDefinition.requiredString("property-" + index));
    }
    String typeName = "T".repeat(130);

    TradingServiceType registered =
        repository.register(new TradingServiceType(typeName, definitions));

    assertEquals(typeName, registered.name());
    assertEquals(65, registered.properties().size());
  }

  @Test
  void rejectsInvalidLimitsNamesAndDuplicateProperties() {
    TradingServiceException invalidLimit =
        assertThrows(TradingServiceException.class, () -> new TradingServiceOptions(0, 1, 1));
    TradingServiceException blankType =
        assertThrows(TradingServiceException.class, () -> TradingServiceType.empty(" "));
    TradingServiceException longName =
        assertThrows(
            TradingServiceException.class,
            () ->
                new LocalTradingTypeRepository(new TradingServiceOptions(2, 2, 4))
                    .register(TradingServiceType.empty("TooLong")));
    TradingServiceException duplicateProperty =
        assertThrows(
            TradingServiceException.class,
            () ->
                new TradingServiceType(
                    "OrderService",
                    List.of(
                        TradingPropertyDefinition.requiredString("symbol"),
                        TradingPropertyDefinition.requiredBoolean("symbol"))));

    assertEquals(TradingServiceDiagnosticCodes.INVALID_LIMIT, invalidLimit.code());
    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_NAME, blankType.code());
    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_NAME, longName.code());
    assertEquals(TradingServiceDiagnosticCodes.DUPLICATE_PROPERTY, duplicateProperty.code());
  }

  @Test
  void rejectsMalformedTypeAndPropertyDefinitions() {
    LocalTradingTypeRepository repository = new LocalTradingTypeRepository();
    List<TradingPropertyDefinition> nullProperty = new ArrayList<>();
    nullProperty.add(null);

    TradingServiceException nullType =
        assertThrows(TradingServiceException.class, () -> repository.register(null));
    TradingServiceException nullProperties =
        assertThrows(TradingServiceException.class, () -> new TradingServiceType("Bad", null));
    TradingServiceException nullPropertyDefinition =
        assertThrows(
            TradingServiceException.class, () -> new TradingServiceType("Bad", nullProperty));
    TradingServiceException nullKind =
        assertThrows(
            TradingServiceException.class, () -> new TradingPropertyDefinition("name", null, true));

    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_TYPE, nullType.code());
    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_TYPE, nullProperties.code());
    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_TYPE, nullPropertyDefinition.code());
    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_TYPE, nullKind.code());
  }

  @Test
  void snapshotsAreImmutableAndIndependentOfCallerLists() {
    LocalTradingTypeRepository repository = new LocalTradingTypeRepository();
    List<TradingPropertyDefinition> definitions = new ArrayList<>();
    definitions.add(TradingPropertyDefinition.requiredString("symbol"));

    TradingServiceType type =
        repository.register(new TradingServiceType("OrderService", definitions));
    definitions.clear();

    assertEquals(1, type.properties().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> type.properties().add(TradingPropertyDefinition.requiredBoolean("active")));
    assertThrows(
        UnsupportedOperationException.class,
        () -> repository.list().add(TradingServiceType.empty("Other")));
  }

  @Test
  void lookupValidatesNamesButMissingTypeIsOptionalEmpty() {
    LocalTradingTypeRepository repository = new LocalTradingTypeRepository();

    assertTrue(repository.lookup("MissingService").isEmpty());
    TradingServiceException malformed =
        assertThrows(TradingServiceException.class, () -> repository.lookup(""));

    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_NAME, malformed.code());
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

  private static TradingServiceType twoPropertyType(String name) {
    return new TradingServiceType(
        name,
        List.of(
            TradingPropertyDefinition.requiredString("symbol"),
            TradingPropertyDefinition.requiredBoolean("active")));
  }
}
