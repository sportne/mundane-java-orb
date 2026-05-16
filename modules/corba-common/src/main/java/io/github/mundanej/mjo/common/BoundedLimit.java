package io.github.mundanej.mjo.common;

import java.util.Objects;
import java.util.Optional;

/**
 * Named nonnegative upper bound for input-derived values.
 *
 * @param name stable limit name used in diagnostics and configuration
 * @param maximum inclusive maximum allowed value
 */
public record BoundedLimit(String name, long maximum) {

  /** Creates a validated bounded limit. */
  public BoundedLimit {
    name = requireNonBlank(name, "name");
    if (maximum < 0) {
      throw new IllegalArgumentException("maximum must be nonnegative");
    }
  }

  /** Returns whether the observed value is within {@code 0..maximum}. */
  public boolean accepts(long observedValue) {
    return observedValue >= 0 && observedValue <= maximum;
  }

  /** Returns a violation when the observed value falls outside {@code 0..maximum}. */
  public Optional<LimitViolation> check(long observedValue) {
    if (accepts(observedValue)) {
      return Optional.empty();
    }
    return Optional.of(new LimitViolation(this, observedValue));
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
