package io.github.mundanej.mjo.security;

import java.util.Objects;

/** Bounded redacted audit field. */
public record SecurityAuditField(String key, String value) {

  /** Maximum audit field key length. */
  public static final int MAX_KEY_LENGTH = 32;

  /** Maximum audit field value length after redaction. */
  public static final int MAX_VALUE_LENGTH = 96;

  /** Creates a bounded, redacted audit field. */
  public SecurityAuditField {
    key = requireBounded(key, "audit field key", MAX_KEY_LENGTH);
    value = SecurityAuditRedaction.redact(key, requireBounded(value, "audit field value", 256));
    if (value.length() > MAX_VALUE_LENGTH) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.AUDIT_EVENT_LIMIT_EXCEEDED,
          "redacted audit field value exceeds " + MAX_VALUE_LENGTH + " characters");
    }
  }

  private static String requireBounded(String value, String label, int maxLength) {
    Objects.requireNonNull(value, label);
    if (value.isBlank()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_AUDIT_EVENT, label + " must not be blank");
    }
    if (value.length() > maxLength) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.AUDIT_EVENT_LIMIT_EXCEEDED,
          label + " exceeds " + maxLength + " characters");
    }
    return value;
  }
}
