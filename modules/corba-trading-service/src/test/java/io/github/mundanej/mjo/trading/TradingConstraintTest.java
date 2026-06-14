package io.github.mundanej.mjo.trading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class TradingConstraintTest {

  @Test
  void evaluatesConstantsAndPropertyEquality() {
    Map<String, Object> properties = properties();

    assertTrue(TradingConstraint.parse("true").evaluate(properties));
    assertFalse(TradingConstraint.parse("false").evaluate(properties));
    assertTrue(TradingConstraint.parse("symbol == 'MJO'").evaluate(properties));
    assertTrue(TradingConstraint.parse("active != false").evaluate(properties));
    assertFalse(TradingConstraint.parse("symbol == 'OTHER'").evaluate(properties));
  }

  @Test
  void evaluatesNumericComparisons() {
    Map<String, Object> properties = properties();

    assertTrue(TradingConstraint.parse("quantity > 41").evaluate(properties));
    assertTrue(TradingConstraint.parse("quantity >= 42").evaluate(properties));
    assertTrue(TradingConstraint.parse("quantity < 43").evaluate(properties));
    assertTrue(TradingConstraint.parse("quantity <= 42").evaluate(properties));
    assertTrue(TradingConstraint.parse("ratio > 0.25").evaluate(properties));
    assertFalse(TradingConstraint.parse("ratio < 0.25").evaluate(properties));
  }

  @Test
  void evaluatesBooleanCompositionAndParenthesesDeterministically() {
    Map<String, Object> properties = properties();
    TradingConstraint constraint =
        TradingConstraint.parse("symbol == 'MJO' and (active == true or quantity < 1)");
    TradingConstraint negated = TradingConstraint.parse("not (active == false)");

    assertTrue(constraint.evaluate(properties));
    assertTrue(negated.evaluate(properties));
    assertEquals(constraint.evaluate(properties), constraint.evaluate(properties));
    assertEquals(2, negated.maxDepth());
    assertTrue(constraint.tokenCount() > constraint.termCount());
  }

  @Test
  void reportsUnknownPropertiesAndTypeMismatchesAtEvaluation() {
    Map<String, Object> properties = properties();

    TradingServiceException unknown =
        assertThrows(
            TradingServiceException.class,
            () -> TradingConstraint.parse("missing == 'value'").evaluate(properties));
    TradingServiceException equalityMismatch =
        assertThrows(
            TradingServiceException.class,
            () -> TradingConstraint.parse("quantity == '42'").evaluate(properties));
    TradingServiceException orderedMismatch =
        assertThrows(
            TradingServiceException.class,
            () -> TradingConstraint.parse("symbol > 'ABC'").evaluate(properties));
    TradingServiceException nullValue =
        assertThrows(
            TradingServiceException.class,
            () ->
                TradingConstraint.parse("symbol == 'MJO'")
                    .evaluate(Map.of("symbol", new UnsupportedValue())));

    assertEquals(TradingServiceDiagnosticCodes.UNKNOWN_CONSTRAINT_PROPERTY, unknown.code());
    assertEquals(TradingServiceDiagnosticCodes.CONSTRAINT_TYPE_MISMATCH, equalityMismatch.code());
    assertEquals(TradingServiceDiagnosticCodes.CONSTRAINT_TYPE_MISMATCH, orderedMismatch.code());
    assertEquals(TradingServiceDiagnosticCodes.UNSUPPORTED_VALUE, nullValue.code());
  }

  @Test
  void rejectsMalformedSyntaxDeterministically() {
    TradingServiceException blank =
        assertThrows(TradingServiceException.class, () -> TradingConstraint.parse(" "));
    TradingServiceException missingLiteral =
        assertThrows(TradingServiceException.class, () -> TradingConstraint.parse("symbol =="));
    TradingServiceException missingParenthesis =
        assertThrows(
            TradingServiceException.class, () -> TradingConstraint.parse("(symbol == 'MJO'"));
    TradingServiceException reserved =
        assertThrows(TradingServiceException.class, () -> TradingConstraint.parse("and == true"));

    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_CONSTRAINT, blank.code());
    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_CONSTRAINT, missingLiteral.code());
    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_CONSTRAINT, missingParenthesis.code());
    assertEquals(TradingServiceDiagnosticCodes.MALFORMED_CONSTRAINT, reserved.code());
  }

  @Test
  void rejectsUnsupportedOperatorsFunctionsWildcardsRegexAndArithmetic() {
    TradingServiceException assignment =
        assertThrows(TradingServiceException.class, () -> TradingConstraint.parse("quantity = 42"));
    TradingServiceException function =
        assertThrows(
            TradingServiceException.class,
            () -> TradingConstraint.parse("matches(symbol) == true"));
    TradingServiceException wildcard =
        assertThrows(TradingServiceException.class, () -> TradingConstraint.parse("symbol == *"));
    TradingServiceException regex =
        assertThrows(
            TradingServiceException.class, () -> TradingConstraint.parse("symbol == /M.*/"));
    TradingServiceException arithmetic =
        assertThrows(
            TradingServiceException.class, () -> TradingConstraint.parse("quantity + 1 > 2"));
    TradingServiceException subtraction =
        assertThrows(
            TradingServiceException.class, () -> TradingConstraint.parse("quantity - 1 > 2"));
    TradingServiceException compactSubtraction =
        assertThrows(
            TradingServiceException.class, () -> TradingConstraint.parse("quantity -1 > 2"));

    assertEquals(TradingServiceDiagnosticCodes.UNSUPPORTED_CONSTRAINT_OPERATOR, assignment.code());
    assertEquals(TradingServiceDiagnosticCodes.UNSUPPORTED_CONSTRAINT_OPERATOR, function.code());
    assertEquals(TradingServiceDiagnosticCodes.UNSUPPORTED_CONSTRAINT_OPERATOR, wildcard.code());
    assertEquals(TradingServiceDiagnosticCodes.UNSUPPORTED_CONSTRAINT_OPERATOR, regex.code());
    assertEquals(TradingServiceDiagnosticCodes.UNSUPPORTED_CONSTRAINT_OPERATOR, arithmetic.code());
    assertEquals(TradingServiceDiagnosticCodes.UNSUPPORTED_CONSTRAINT_OPERATOR, subtraction.code());
    assertEquals(
        TradingServiceDiagnosticCodes.UNSUPPORTED_CONSTRAINT_OPERATOR, compactSubtraction.code());
  }

  @Test
  void rejectsExpressionDepthTokenTermAndLiteralLimits() {
    TradingServiceException tooLong =
        assertThrows(
            TradingServiceException.class,
            () -> TradingConstraint.parse("x".repeat(TradingConstraint.MAX_EXPRESSION_LENGTH + 1)));
    TradingServiceException tooDeep =
        assertThrows(
            TradingServiceException.class,
            () ->
                TradingConstraint.parse(
                    "(".repeat(TradingConstraint.MAX_DEPTH + 1)
                        + "true"
                        + ")".repeat(TradingConstraint.MAX_DEPTH + 1)));
    TradingServiceException tooManyTerms =
        assertThrows(TradingServiceException.class, () -> TradingConstraint.parse(manyTerms()));
    TradingServiceException tooManyTokens =
        assertThrows(TradingServiceException.class, () -> TradingConstraint.parse(manyTokens()));
    TradingServiceException tooLongString =
        assertThrows(
            TradingServiceException.class,
            () ->
                TradingConstraint.parse(
                    "symbol == '"
                        + "x".repeat(TradingConstraint.MAX_STRING_LITERAL_LENGTH + 1)
                        + "'"));

    assertEquals(TradingServiceDiagnosticCodes.CONSTRAINT_LIMIT_EXCEEDED, tooLong.code());
    assertEquals(TradingServiceDiagnosticCodes.CONSTRAINT_LIMIT_EXCEEDED, tooDeep.code());
    assertEquals(TradingServiceDiagnosticCodes.CONSTRAINT_LIMIT_EXCEEDED, tooManyTerms.code());
    assertEquals(TradingServiceDiagnosticCodes.CONSTRAINT_LIMIT_EXCEEDED, tooManyTokens.code());
    assertEquals(TradingServiceDiagnosticCodes.CONSTRAINT_LIMIT_EXCEEDED, tooLongString.code());
  }

  @Test
  void rejectsNonFiniteFloatingPointLiteralsAndValues() {
    TradingServiceException literal =
        assertThrows(
            TradingServiceException.class,
            () -> TradingConstraint.parse("ratio == " + "9".repeat(309) + ".0"));
    TradingServiceException value =
        assertThrows(
            TradingServiceException.class,
            () ->
                TradingConstraint.parse("ratio == 1.0")
                    .evaluate(Map.of("ratio", Double.POSITIVE_INFINITY)));

    assertEquals(TradingServiceDiagnosticCodes.UNSUPPORTED_VALUE, literal.code());
    assertEquals(TradingServiceDiagnosticCodes.UNSUPPORTED_VALUE, value.code());
  }

  private static Map<String, Object> properties() {
    return Map.of("symbol", "MJO", "active", true, "quantity", 42L, "ratio", 0.5D);
  }

  private static String manyTerms() {
    StringBuilder builder = new StringBuilder("true");
    for (int index = 0; index < TradingConstraint.MAX_TERMS; index++) {
      builder.append(" or true");
    }
    return builder.toString();
  }

  private static String manyTokens() {
    StringBuilder builder = new StringBuilder("a == 1");
    for (int index = 0; index < 43; index++) {
      builder.append(" and a == 1");
    }
    return builder.toString();
  }

  private static final class UnsupportedValue {}
}
