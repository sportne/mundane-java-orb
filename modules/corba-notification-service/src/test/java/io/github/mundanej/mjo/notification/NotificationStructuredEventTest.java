package io.github.mundanej.mjo.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class NotificationStructuredEventTest {

  @Test
  void createsValidStructuredEvents() {
    NotificationStructuredEvent event =
        new NotificationStructuredEvent(
            identity(),
            List.of(NotificationProperty.stringProperty("symbol", "MJO")),
            List.of(NotificationProperty.signedLongProperty("priority", 5L)),
            List.of(NotificationProperty.booleanProperty("active", true)));

    assertEquals("demo", event.identity().eventType().domainName());
    assertEquals("price", event.identity().eventType().typeName());
    assertEquals("quote", event.identity().eventName());
    assertEquals("MJO", event.filterProperties().get(0).value().asString());
    assertEquals(5L, event.variableHeaderFields().get(0).value().asSignedLong());
    assertEquals(true, event.bodyFields().get(0).value().asBoolean());
  }

  @Test
  void supportsPrimitivePropertyKinds() {
    NotificationProperty text = NotificationProperty.stringProperty("text", "hello");
    NotificationProperty flag = NotificationProperty.booleanProperty("flag", true);
    NotificationProperty count = NotificationProperty.signedLongProperty("count", 42L);
    NotificationProperty ratio = NotificationProperty.floatingPointProperty("ratio", 0.25d);

    assertEquals(NotificationPrimitiveKind.STRING, text.value().kind());
    assertEquals("hello", text.value().asString());
    assertEquals(true, flag.value().asBoolean());
    assertEquals(42L, count.value().asSignedLong());
    assertEquals(0.25d, ratio.value().asFloatingPoint());
  }

  @Test
  void rejectsDuplicatePropertiesInOneSection() {
    NotificationServiceException exception =
        assertThrows(
            NotificationServiceException.class,
            () ->
                new NotificationStructuredEvent(
                    identity(),
                    List.of(
                        NotificationProperty.stringProperty("symbol", "MJO"),
                        NotificationProperty.stringProperty("symbol", "ORB")),
                    List.of(),
                    List.of()));

    assertEquals(NotificationServiceDiagnosticCodes.DUPLICATE_FIELD, exception.code());
  }

  @Test
  void rejectsOversizedSectionsAndStrings() {
    List<NotificationProperty> properties = new ArrayList<>();
    for (int index = 0; index <= NotificationStructuredEvent.MAX_FIELDS_PER_SECTION; index++) {
      properties.add(NotificationProperty.signedLongProperty("p" + index, index));
    }

    NotificationServiceException section =
        assertThrows(
            NotificationServiceException.class,
            () -> new NotificationStructuredEvent(identity(), properties, List.of(), List.of()));
    NotificationServiceException string =
        assertThrows(
            NotificationServiceException.class,
            () ->
                NotificationProperty.stringProperty(
                    "payload", "x".repeat(NotificationPrimitiveValue.MAX_STRING_LENGTH + 1)));

    assertEquals(NotificationServiceDiagnosticCodes.FIELD_LIMIT_EXCEEDED, section.code());
    assertEquals(NotificationServiceDiagnosticCodes.VALUE_LIMIT_EXCEEDED, string.code());
  }

  @Test
  void rejectsMalformedIdentityAndPropertyNames() {
    NotificationServiceException identity =
        assertThrows(
            NotificationServiceException.class,
            () -> new NotificationEventIdentity(new NotificationEventType("demo", "price"), " "));
    NotificationServiceException property =
        assertThrows(
            NotificationServiceException.class,
            () -> NotificationProperty.booleanProperty("", true));

    assertEquals(NotificationServiceDiagnosticCodes.MALFORMED_STRUCTURED_EVENT, identity.code());
    assertEquals(NotificationServiceDiagnosticCodes.MALFORMED_STRUCTURED_EVENT, property.code());
  }

  @Test
  void rejectsNullStructuredEventPartsWithDiagnostics() {
    NotificationServiceException missingIdentity =
        assertThrows(
            NotificationServiceException.class,
            () -> new NotificationStructuredEvent(null, List.of(), List.of(), List.of()));
    NotificationServiceException missingSection =
        assertThrows(
            NotificationServiceException.class,
            () -> new NotificationStructuredEvent(identity(), null, List.of(), List.of()));
    NotificationServiceException missingEventType =
        assertThrows(
            NotificationServiceException.class, () -> new NotificationEventIdentity(null, "quote"));
    NotificationServiceException missingName =
        assertThrows(
            NotificationServiceException.class,
            () -> NotificationProperty.booleanProperty(null, true));
    List<NotificationProperty> nullProperty = new ArrayList<>();
    nullProperty.add(null);
    NotificationServiceException missingProperty =
        assertThrows(
            NotificationServiceException.class,
            () -> new NotificationStructuredEvent(identity(), nullProperty, List.of(), List.of()));

    assertEquals(
        NotificationServiceDiagnosticCodes.MALFORMED_STRUCTURED_EVENT, missingIdentity.code());
    assertEquals(
        NotificationServiceDiagnosticCodes.MALFORMED_STRUCTURED_EVENT, missingSection.code());
    assertEquals(
        NotificationServiceDiagnosticCodes.MALFORMED_STRUCTURED_EVENT, missingEventType.code());
    assertEquals(NotificationServiceDiagnosticCodes.MALFORMED_STRUCTURED_EVENT, missingName.code());
    assertEquals(
        NotificationServiceDiagnosticCodes.MALFORMED_STRUCTURED_EVENT, missingProperty.code());
  }

  @Test
  void rejectsUnsupportedPrimitiveRepresentations() {
    NotificationServiceException wrongType =
        assertThrows(
            NotificationServiceException.class,
            () ->
                new NotificationPrimitiveValue(NotificationPrimitiveKind.STRING, Long.valueOf(1)));
    NotificationServiceException nonFinite =
        assertThrows(
            NotificationServiceException.class,
            () -> NotificationPrimitiveValue.floatingPointValue(Double.NaN));
    NotificationServiceException nullKind =
        assertThrows(
            NotificationServiceException.class, () -> new NotificationPrimitiveValue(null, "text"));
    NotificationServiceException nullValue =
        assertThrows(
            NotificationServiceException.class,
            () -> new NotificationPrimitiveValue(NotificationPrimitiveKind.STRING, null));

    assertEquals(NotificationServiceDiagnosticCodes.UNSUPPORTED_VALUE, wrongType.code());
    assertEquals(NotificationServiceDiagnosticCodes.UNSUPPORTED_VALUE, nonFinite.code());
    assertEquals(NotificationServiceDiagnosticCodes.UNSUPPORTED_VALUE, nullKind.code());
    assertEquals(NotificationServiceDiagnosticCodes.UNSUPPORTED_VALUE, nullValue.code());
  }

  @Test
  void structuredEventsAreImmutable() {
    List<NotificationProperty> properties = new ArrayList<>();
    properties.add(NotificationProperty.stringProperty("symbol", "MJO"));

    NotificationStructuredEvent event =
        new NotificationStructuredEvent(identity(), properties, List.of(), List.of());
    properties.clear();

    assertFalse(event.filterProperties().isEmpty());
    assertThrows(
        UnsupportedOperationException.class,
        () -> event.filterProperties().add(NotificationProperty.stringProperty("extra", "value")));
  }

  private static NotificationEventIdentity identity() {
    return new NotificationEventIdentity(new NotificationEventType("demo", "price"), "quote");
  }
}
