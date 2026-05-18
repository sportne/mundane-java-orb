package io.github.mundanej.mjo.interop.testkit;

import java.time.Instant;
import java.util.Objects;

/** Immutable command result captured from a peer container invocation. */
public record InteropCommandResult(
    int exitCode, String stdoutPath, String stderrPath, Instant startedAt, Instant endedAt) {
  public InteropCommandResult {
    requireNotBlank(stdoutPath, "stdoutPath");
    requireNotBlank(stderrPath, "stderrPath");
    Objects.requireNonNull(startedAt, "startedAt");
    Objects.requireNonNull(endedAt, "endedAt");
    if (endedAt.isBefore(startedAt)) {
      throw new IllegalArgumentException("endedAt must not be before startedAt");
    }
  }

  private static void requireNotBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }
}
