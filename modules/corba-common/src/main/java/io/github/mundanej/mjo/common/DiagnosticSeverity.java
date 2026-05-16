package io.github.mundanej.mjo.common;

/** Severity for a diagnostic emitted by parser, semantic, protocol, or tooling code. */
public enum DiagnosticSeverity {
  /** Informational diagnostic that does not indicate invalid input. */
  INFO,
  /** Recoverable diagnostic that should be visible to callers. */
  WARNING,
  /** Error diagnostic for invalid input or failed validation. */
  ERROR
}
