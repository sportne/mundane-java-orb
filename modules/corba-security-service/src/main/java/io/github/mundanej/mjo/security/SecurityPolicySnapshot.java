package io.github.mundanej.mjo.security;

import java.util.List;

/** Immutable validated Security Service policy snapshot. */
public record SecurityPolicySnapshot(
    SecurityAuthenticationRequirement authentication,
    SecurityTrustRequirement trust,
    SecurityTransportProtection transportProtection,
    SecurityIdentityAssertionMode identityAssertion,
    SecurityDelegationMode delegation,
    SecurityAuditLevel audit,
    List<SecurityPolicySetting> settings) {

  /** Creates an immutable validated policy snapshot. */
  public SecurityPolicySnapshot {
    if (authentication == null
        || trust == null
        || transportProtection == null
        || identityAssertion == null
        || delegation == null
        || audit == null
        || settings == null) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_POLICY, "policy snapshot must be complete");
    }
    settings =
        List.of(
            SecurityPolicySetting.authentication(authentication),
            SecurityPolicySetting.trust(trust),
            SecurityPolicySetting.transport(transportProtection),
            SecurityPolicySetting.identityAssertion(identityAssertion),
            SecurityPolicySetting.delegation(delegation),
            SecurityPolicySetting.audit(audit));
  }
}
