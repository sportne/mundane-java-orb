package io.github.mundanej.mjo.security;

import java.util.Locale;

final class SecurityAuditRedaction {

  static final String REDACTED = "[REDACTED]";

  private SecurityAuditRedaction() {}

  static String redact(String key, String value) {
    String lowerKey = key.toLowerCase(Locale.ROOT);
    String lowerValue = value.toLowerCase(Locale.ROOT);
    if (isSensitiveKey(lowerKey) || isSensitiveValue(lowerValue)) {
      return REDACTED;
    }
    return value;
  }

  private static boolean isSensitiveKey(String value) {
    return value.contains("authorization")
        || value.contains("bearer")
        || value.contains("credential")
        || value.contains("password")
        || value.contains("secret")
        || value.contains("token");
  }

  private static boolean isSensitiveValue(String value) {
    return value.contains("authorization")
        || value.contains("bearer")
        || value.contains("credential")
        || value.contains("password")
        || value.contains("secret")
        || value.contains("token");
  }
}
