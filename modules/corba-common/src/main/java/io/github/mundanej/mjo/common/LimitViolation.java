package io.github.mundanej.mjo.common;

import java.util.Objects;

/**
 * Failed bounded-limit check.
 *
 * @param limit limit that rejected the observed value
 * @param observedValue rejected value
 */
public record LimitViolation(BoundedLimit limit, long observedValue) {

  /** Creates a validated limit violation. */
  public LimitViolation {
    Objects.requireNonNull(limit, "limit");
  }

  /** Returns a deterministic message for logs and diagnostics. */
  public String message() {
    return "Limit '%s' rejected value %d; allowed range is 0..%d"
        .formatted(limit.name(), observedValue, limit.maximum());
  }
}
