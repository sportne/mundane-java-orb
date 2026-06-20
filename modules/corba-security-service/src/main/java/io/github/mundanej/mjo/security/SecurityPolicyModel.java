package io.github.mundanej.mjo.security;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Validation-only policy model for the supported local Security Service subset. */
public final class SecurityPolicyModel {

  private final SecurityPolicyOptions options;

  /** Creates a policy model with default validation limits. */
  public SecurityPolicyModel() {
    this(SecurityPolicyOptions.defaults());
  }

  /** Creates a policy model with caller-provided validation limits. */
  public SecurityPolicyModel(SecurityPolicyOptions options) {
    this.options = Objects.requireNonNull(options, "options");
  }

  /** Returns the default policy snapshot for the supported local subset. */
  public SecurityPolicySnapshot defaults() {
    return validate(List.of());
  }

  /** Validates supplied policy settings and returns a deterministic typed snapshot. */
  public SecurityPolicySnapshot validate(List<SecurityPolicySetting> settings) {
    if (settings == null) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_POLICY, "policy settings must not be null");
    }
    if (settings.size() > options.maxPolicySettings()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.POLICY_LIMIT_EXCEEDED,
          "policy settings exceed " + options.maxPolicySettings() + " entries");
    }
    Map<SecurityPolicyKey, SecurityPolicySetting> byKey = new EnumMap<>(SecurityPolicyKey.class);
    for (SecurityPolicySetting setting : settings) {
      if (setting == null) {
        throw new SecurityServiceException(
            SecurityServiceDiagnosticCodes.MALFORMED_POLICY, "policy setting must not be null");
      }
      SecurityPolicySetting nonNull = setting;
      if (byKey.putIfAbsent(nonNull.key(), nonNull) != null) {
        throw new SecurityServiceException(
            SecurityServiceDiagnosticCodes.DUPLICATE_POLICY,
            "duplicate policy key: " + nonNull.key());
      }
    }

    SecurityAuthenticationRequirement authentication =
        parse(
            byKey.get(SecurityPolicyKey.AUTHENTICATION),
            SecurityAuthenticationRequirement.OPTIONAL,
            SecurityAuthenticationRequirement.class);
    SecurityTrustRequirement trust =
        parse(
            byKey.get(SecurityPolicyKey.TRUST),
            SecurityTrustRequirement.OPTIONAL,
            SecurityTrustRequirement.class);
    SecurityTransportProtection transport =
        parse(
            byKey.get(SecurityPolicyKey.TRANSPORT_PROTECTION),
            SecurityTransportProtection.NONE,
            SecurityTransportProtection.class);
    SecurityIdentityAssertionMode identityAssertion =
        parse(
            byKey.get(SecurityPolicyKey.IDENTITY_ASSERTION),
            SecurityIdentityAssertionMode.DISABLED,
            SecurityIdentityAssertionMode.class);
    SecurityDelegationMode delegation =
        parse(
            byKey.get(SecurityPolicyKey.DELEGATION),
            SecurityDelegationMode.DISABLED,
            SecurityDelegationMode.class);
    SecurityAuditLevel audit =
        parse(byKey.get(SecurityPolicyKey.AUDIT), SecurityAuditLevel.OFF, SecurityAuditLevel.class);

    if (delegation == SecurityDelegationMode.ENABLED) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.UNSUPPORTED_DELEGATION,
          "delegation is outside the supported Security Service subset");
    }
    if (trust == SecurityTrustRequirement.REQUIRED
        && authentication == SecurityAuthenticationRequirement.OPTIONAL) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.CONFLICTING_POLICY,
          "trust-required policy requires authentication-required policy");
    }
    if (identityAssertion == SecurityIdentityAssertionMode.SELF
        && authentication == SecurityAuthenticationRequirement.OPTIONAL) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.CONFLICTING_POLICY,
          "identity assertion requires authentication-required policy");
    }

    return new SecurityPolicySnapshot(
        authentication,
        trust,
        transport,
        identityAssertion,
        delegation,
        audit,
        ordered(authentication, trust, transport, identityAssertion, delegation, audit));
  }

  private static List<SecurityPolicySetting> ordered(
      SecurityAuthenticationRequirement authentication,
      SecurityTrustRequirement trust,
      SecurityTransportProtection transport,
      SecurityIdentityAssertionMode identityAssertion,
      SecurityDelegationMode delegation,
      SecurityAuditLevel audit) {
    return List.of(
        SecurityPolicySetting.authentication(authentication),
        SecurityPolicySetting.trust(trust),
        SecurityPolicySetting.transport(transport),
        SecurityPolicySetting.identityAssertion(identityAssertion),
        SecurityPolicySetting.delegation(delegation),
        SecurityPolicySetting.audit(audit));
  }

  private static <T extends Enum<T>> T parse(
      SecurityPolicySetting setting, T defaultValue, Class<T> enumType) {
    if (setting == null) {
      return defaultValue;
    }
    try {
      return Enum.valueOf(enumType, setting.value());
    } catch (IllegalArgumentException ex) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_POLICY,
          "unsupported policy value for " + setting.key());
    }
  }
}
