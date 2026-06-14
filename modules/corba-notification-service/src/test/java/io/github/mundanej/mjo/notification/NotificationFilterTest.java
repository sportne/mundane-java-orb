package io.github.mundanej.mjo.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class NotificationFilterTest {

  @Test
  void evaluatesConstantsAndIdentityComparisons() {
    NotificationStructuredEvent event = event();

    assertTrue(NotificationFilter.parse("true").evaluate(event));
    assertFalse(NotificationFilter.parse("false").evaluate(event));
    assertTrue(NotificationFilter.parse("domain_name == 'market'").evaluate(event));
    assertTrue(NotificationFilter.parse("type_name != 'trade'").evaluate(event));
    assertTrue(NotificationFilter.parse("event_name == 'quote'").evaluate(event));
  }

  @Test
  void evaluatesPrimitiveFilterProperties() {
    NotificationStructuredEvent event = event();

    assertTrue(NotificationFilter.parse("filter.symbol == 'MJO'").evaluate(event));
    assertTrue(NotificationFilter.parse("filter.active == true").evaluate(event));
    assertTrue(NotificationFilter.parse("filter.quantity == 42").evaluate(event));
    assertTrue(NotificationFilter.parse("filter.ratio == 0.5").evaluate(event));
    assertFalse(NotificationFilter.parse("filter.quantity == 43").evaluate(event));
  }

  @Test
  void evaluatesBooleanCompositionDeterministically() {
    NotificationStructuredEvent event = event();
    NotificationFilter filter =
        NotificationFilter.parse(
            "domain_name == 'market' and (filter.symbol == 'MJO' or filter.active == false)");
    NotificationFilter negated = NotificationFilter.parse("not (filter.active == false)");

    assertTrue(filter.evaluate(event));
    assertTrue(negated.evaluate(event));
    assertEquals(filter.evaluate(event), filter.evaluate(event));
  }

  @Test
  void rejectsUnsupportedOperators() {
    NotificationServiceException exception =
        assertThrows(
            NotificationServiceException.class,
            () -> NotificationFilter.parse("filter.quantity > 10"));

    assertEquals(NotificationServiceDiagnosticCodes.UNSUPPORTED_FILTER_OPERATOR, exception.code());
  }

  @Test
  void rejectsMalformedSyntax() {
    NotificationServiceException missingLiteral =
        assertThrows(
            NotificationServiceException.class, () -> NotificationFilter.parse("filter.symbol =="));
    NotificationServiceException missingParenthesis =
        assertThrows(
            NotificationServiceException.class,
            () -> NotificationFilter.parse("(filter.symbol == 'MJO'"));

    assertEquals(NotificationServiceDiagnosticCodes.MALFORMED_FILTER, missingLiteral.code());
    assertEquals(NotificationServiceDiagnosticCodes.MALFORMED_FILTER, missingParenthesis.code());
  }

  @Test
  void rejectsExpressionLimits() {
    NotificationServiceException tooLong =
        assertThrows(
            NotificationServiceException.class,
            () ->
                NotificationFilter.parse("x".repeat(NotificationFilter.MAX_EXPRESSION_LENGTH + 1)));
    NotificationServiceException tooDeep =
        assertThrows(
            NotificationServiceException.class,
            () ->
                NotificationFilter.parse(
                    "(".repeat(NotificationFilter.MAX_DEPTH + 1)
                        + "true"
                        + ")".repeat(NotificationFilter.MAX_DEPTH + 1)));
    NotificationServiceException tooManyTerms =
        assertThrows(
            NotificationServiceException.class, () -> NotificationFilter.parse(manyTerms()));

    assertEquals(NotificationServiceDiagnosticCodes.FILTER_LIMIT_EXCEEDED, tooLong.code());
    assertEquals(NotificationServiceDiagnosticCodes.FILTER_LIMIT_EXCEEDED, tooDeep.code());
    assertEquals(NotificationServiceDiagnosticCodes.FILTER_LIMIT_EXCEEDED, tooManyTerms.code());
  }

  @Test
  void reportsUnknownFieldsAndTypeMismatchAtEvaluation() {
    NotificationStructuredEvent event = event();

    NotificationServiceException unknown =
        assertThrows(
            NotificationServiceException.class,
            () -> NotificationFilter.parse("filter.missing == 'value'").evaluate(event));
    NotificationServiceException wrongType =
        assertThrows(
            NotificationServiceException.class,
            () -> NotificationFilter.parse("filter.quantity == '42'").evaluate(event));
    NotificationServiceException shortCircuitUnknown =
        assertThrows(
            NotificationServiceException.class,
            () -> NotificationFilter.parse("true or filter.missing == 'value'").evaluate(event));
    NotificationServiceException shortCircuitWrongType =
        assertThrows(
            NotificationServiceException.class,
            () -> NotificationFilter.parse("false and filter.quantity == '42'").evaluate(event));

    assertEquals(NotificationServiceDiagnosticCodes.UNKNOWN_FILTER_FIELD, unknown.code());
    assertEquals(NotificationServiceDiagnosticCodes.FILTER_TYPE_MISMATCH, wrongType.code());
    assertEquals(
        NotificationServiceDiagnosticCodes.UNKNOWN_FILTER_FIELD, shortCircuitUnknown.code());
    assertEquals(
        NotificationServiceDiagnosticCodes.FILTER_TYPE_MISMATCH, shortCircuitWrongType.code());
  }

  @Test
  void rejectsNonFiniteFloatingPointLiterals() {
    NotificationServiceException exception =
        assertThrows(
            NotificationServiceException.class,
            () -> NotificationFilter.parse("filter.ratio == " + "9".repeat(309) + ".0"));

    assertEquals(NotificationServiceDiagnosticCodes.UNSUPPORTED_VALUE, exception.code());
  }

  private static NotificationStructuredEvent event() {
    return new NotificationStructuredEvent(
        new NotificationEventIdentity(new NotificationEventType("market", "quote"), "quote"),
        List.of(
            NotificationProperty.stringProperty("symbol", "MJO"),
            NotificationProperty.booleanProperty("active", true),
            NotificationProperty.signedLongProperty("quantity", 42L),
            NotificationProperty.floatingPointProperty("ratio", 0.5d)),
        List.of(),
        List.of());
  }

  private static String manyTerms() {
    StringBuilder builder = new StringBuilder("true");
    for (int index = 0; index < NotificationFilter.MAX_TERMS; index++) {
      builder.append(" or true");
    }
    return builder.toString();
  }
}
