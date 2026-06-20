package io.github.mundanej.mjo.security;

/** Redacted audit level policy values. */
public enum SecurityAuditLevel {
  /** Audit recording is disabled. */
  OFF,

  /** Only denial diagnostics are recorded by later audit slices. */
  DENIALS,

  /** All supported security decisions are eligible for redacted audit records. */
  ALL
}
