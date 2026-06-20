package io.github.mundanej.mjo.security;

/** Caller-configurable redacted audit event bounds. */
public record SecurityAuditOptions(int maxFields, int maxFailures) {

  /** Default audit field limit. */
  public static final int DEFAULT_MAX_FIELDS = 8;

  /** Default failure disclosure limit. */
  public static final int DEFAULT_MAX_FAILURES = 8;

  /** Absolute audit field limit. */
  public static final int ABSOLUTE_MAX_FIELDS = 32;

  /** Absolute failure disclosure limit. */
  public static final int ABSOLUTE_MAX_FAILURES = 32;

  /** Creates validated audit bounds. */
  public SecurityAuditOptions {
    requireRange(maxFields, 0, ABSOLUTE_MAX_FIELDS, "audit field limit");
    requireRange(maxFailures, 1, ABSOLUTE_MAX_FAILURES, "failure disclosure limit");
  }

  /** Returns default audit bounds. */
  public static SecurityAuditOptions defaults() {
    return new SecurityAuditOptions(DEFAULT_MAX_FIELDS, DEFAULT_MAX_FAILURES);
  }

  private static void requireRange(int value, int min, int max, String label) {
    if (value < min || value > max) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.INVALID_LIMIT,
          label + " must be between " + min + " and " + max);
    }
  }
}
