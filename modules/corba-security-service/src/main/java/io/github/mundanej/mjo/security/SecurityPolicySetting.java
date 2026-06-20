package io.github.mundanej.mjo.security;

import java.util.Locale;

/** One bounded Security Service policy setting supplied for validation. */
public record SecurityPolicySetting(SecurityPolicyKey key, String value) {

  /** Maximum normalized token length accepted for a policy value. */
  public static final int MAX_VALUE_LENGTH = 64;

  /** Creates a policy setting with a nonblank normalized value token. */
  public SecurityPolicySetting {
    if (key == null) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_POLICY, "policy key must not be null");
    }
    if (value == null || value.isBlank()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_POLICY, "policy value must not be blank");
    }
    if (value.length() > MAX_VALUE_LENGTH) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.POLICY_LIMIT_EXCEEDED,
          "policy value exceeds " + MAX_VALUE_LENGTH + " characters");
    }
    value = value.toUpperCase(Locale.ROOT);
  }

  /** Creates a typed authentication setting. */
  public static SecurityPolicySetting authentication(SecurityAuthenticationRequirement value) {
    return new SecurityPolicySetting(SecurityPolicyKey.AUTHENTICATION, requireName(value));
  }

  /** Creates a typed trust setting. */
  public static SecurityPolicySetting trust(SecurityTrustRequirement value) {
    return new SecurityPolicySetting(SecurityPolicyKey.TRUST, requireName(value));
  }

  /** Creates a typed transport-protection setting. */
  public static SecurityPolicySetting transport(SecurityTransportProtection value) {
    return new SecurityPolicySetting(SecurityPolicyKey.TRANSPORT_PROTECTION, requireName(value));
  }

  /** Creates a typed identity-assertion setting. */
  public static SecurityPolicySetting identityAssertion(SecurityIdentityAssertionMode value) {
    return new SecurityPolicySetting(SecurityPolicyKey.IDENTITY_ASSERTION, requireName(value));
  }

  /** Creates a typed delegation setting. */
  public static SecurityPolicySetting delegation(SecurityDelegationMode value) {
    return new SecurityPolicySetting(SecurityPolicyKey.DELEGATION, requireName(value));
  }

  /** Creates a typed audit-level setting. */
  public static SecurityPolicySetting audit(SecurityAuditLevel value) {
    return new SecurityPolicySetting(SecurityPolicyKey.AUDIT, requireName(value));
  }

  private static String requireName(Enum<?> value) {
    if (value == null) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_POLICY, "policy value must not be null");
    }
    return value.name();
  }
}
