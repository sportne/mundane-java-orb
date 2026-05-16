package io.github.mundanej.mjo.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link BoundedLimit} and {@link LimitViolation}. */
@Tag("unit")
final class BoundedLimitTest {

  @Test
  void acceptsValuesInInclusiveRange() {
    BoundedLimit limit = new BoundedLimit("message-size", 10);

    assertTrue(limit.accepts(0));
    assertTrue(limit.accepts(10));
    assertFalse(limit.accepts(11));
    assertFalse(limit.accepts(-1));
  }

  @Test
  void rejectsInvalidLimitDefinition() {
    assertThrows(IllegalArgumentException.class, () -> new BoundedLimit(" ", 1));
    assertThrows(IllegalArgumentException.class, () -> new BoundedLimit("message-size", -1));
  }

  @Test
  void returnsEmptyCheckForAcceptedValue() {
    BoundedLimit limit = new BoundedLimit("message-size", 10);

    assertEquals(Optional.empty(), limit.check(7));
  }

  @Test
  void returnsViolationForRejectedValue() {
    BoundedLimit limit = new BoundedLimit("message-size", 10);

    LimitViolation violation = limit.check(11).orElseThrow();

    assertEquals(limit, violation.limit());
    assertEquals(11, violation.observedValue());
    assertEquals(
        "Limit 'message-size' rejected value 11; allowed range is 0..10", violation.message());
  }

  @Test
  void violationsHaveValueEquality() {
    BoundedLimit limit = new BoundedLimit("sequence-length", 3);

    assertEquals(new LimitViolation(limit, 4), new LimitViolation(limit, 4));
  }
}
