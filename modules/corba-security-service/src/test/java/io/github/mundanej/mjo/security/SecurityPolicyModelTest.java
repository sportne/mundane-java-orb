package io.github.mundanej.mjo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SecurityPolicyModelTest {

  @Test
  void returnsDefaultPoliciesInDeterministicOrder() {
    SecurityPolicySnapshot snapshot = new SecurityPolicyModel().defaults();

    assertEquals(SecurityAuthenticationRequirement.OPTIONAL, snapshot.authentication());
    assertEquals(SecurityTrustRequirement.OPTIONAL, snapshot.trust());
    assertEquals(SecurityTransportProtection.NONE, snapshot.transportProtection());
    assertEquals(SecurityIdentityAssertionMode.DISABLED, snapshot.identityAssertion());
    assertEquals(SecurityDelegationMode.DISABLED, snapshot.delegation());
    assertEquals(SecurityAuditLevel.OFF, snapshot.audit());
    assertEquals(
        List.of(
            SecurityPolicyKey.AUTHENTICATION,
            SecurityPolicyKey.TRUST,
            SecurityPolicyKey.TRANSPORT_PROTECTION,
            SecurityPolicyKey.IDENTITY_ASSERTION,
            SecurityPolicyKey.DELEGATION,
            SecurityPolicyKey.AUDIT),
        snapshot.settings().stream().map(SecurityPolicySetting::key).toList());
  }

  @Test
  void validatesExplicitPolicySubset() {
    SecurityPolicySnapshot snapshot =
        new SecurityPolicyModel()
            .validate(
                List.of(
                    SecurityPolicySetting.authentication(
                        SecurityAuthenticationRequirement.REQUIRED),
                    SecurityPolicySetting.trust(SecurityTrustRequirement.REQUIRED),
                    SecurityPolicySetting.transport(SecurityTransportProtection.CONFIDENTIALITY),
                    SecurityPolicySetting.identityAssertion(SecurityIdentityAssertionMode.SELF),
                    SecurityPolicySetting.delegation(SecurityDelegationMode.DISABLED),
                    SecurityPolicySetting.audit(SecurityAuditLevel.DENIALS)));

    assertEquals(SecurityAuthenticationRequirement.REQUIRED, snapshot.authentication());
    assertEquals(SecurityTrustRequirement.REQUIRED, snapshot.trust());
    assertEquals(SecurityTransportProtection.CONFIDENTIALITY, snapshot.transportProtection());
    assertEquals(SecurityIdentityAssertionMode.SELF, snapshot.identityAssertion());
    assertEquals(SecurityDelegationMode.DISABLED, snapshot.delegation());
    assertEquals(SecurityAuditLevel.DENIALS, snapshot.audit());
  }

  @Test
  void rejectsDuplicatePolicyKeys() {
    SecurityServiceException duplicate =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityPolicyModel()
                    .validate(
                        List.of(
                            SecurityPolicySetting.audit(SecurityAuditLevel.OFF),
                            SecurityPolicySetting.audit(SecurityAuditLevel.ALL))));

    assertEquals(SecurityServiceDiagnosticCodes.DUPLICATE_POLICY, duplicate.code());
  }

  @Test
  void rejectsUnsupportedDelegation() {
    SecurityServiceException unsupported =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityPolicyModel()
                    .validate(
                        List.of(SecurityPolicySetting.delegation(SecurityDelegationMode.ENABLED))));

    assertEquals(SecurityServiceDiagnosticCodes.UNSUPPORTED_DELEGATION, unsupported.code());
  }

  @Test
  void rejectsConflictingPolicyCombinations() {
    SecurityServiceException trustWithoutAuthentication =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityPolicyModel()
                    .validate(
                        List.of(SecurityPolicySetting.trust(SecurityTrustRequirement.REQUIRED))));
    SecurityServiceException assertionWithoutAuthentication =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityPolicyModel()
                    .validate(
                        List.of(
                            SecurityPolicySetting.identityAssertion(
                                SecurityIdentityAssertionMode.SELF))));

    assertEquals(
        SecurityServiceDiagnosticCodes.CONFLICTING_POLICY, trustWithoutAuthentication.code());
    assertEquals(
        SecurityServiceDiagnosticCodes.CONFLICTING_POLICY, assertionWithoutAuthentication.code());
  }

  @Test
  void rejectsMalformedPolicyValues() {
    SecurityServiceException malformedValue =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityPolicyModel()
                    .validate(
                        List.of(
                            new SecurityPolicySetting(
                                SecurityPolicyKey.TRANSPORT_PROTECTION, "telepathy"))));
    SecurityServiceException oversizeValue =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityPolicySetting(
                    SecurityPolicyKey.AUDIT,
                    "x".repeat(SecurityPolicySetting.MAX_VALUE_LENGTH + 1)));
    SecurityServiceException malformedKey =
        assertThrows(SecurityServiceException.class, () -> new SecurityPolicySetting(null, "off"));
    SecurityServiceException blankValue =
        assertThrows(
            SecurityServiceException.class,
            () -> new SecurityPolicySetting(SecurityPolicyKey.AUDIT, " "));
    SecurityServiceException nullValue =
        assertThrows(
            SecurityServiceException.class,
            () -> new SecurityPolicySetting(SecurityPolicyKey.AUDIT, null));
    SecurityServiceException nullTypedValue =
        assertThrows(SecurityServiceException.class, () -> SecurityPolicySetting.audit(null));
    SecurityServiceException nullSettings =
        assertThrows(
            SecurityServiceException.class, () -> new SecurityPolicyModel().validate(null));
    SecurityServiceException nullSetting =
        assertThrows(
            SecurityServiceException.class,
            () -> new SecurityPolicyModel().validate(Collections.singletonList(null)));

    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_POLICY, malformedValue.code());
    assertFalse(malformedValue.getMessage().contains("telepathy"));
    assertEquals(SecurityServiceDiagnosticCodes.POLICY_LIMIT_EXCEEDED, oversizeValue.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_POLICY, malformedKey.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_POLICY, blankValue.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_POLICY, nullValue.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_POLICY, nullTypedValue.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_POLICY, nullSettings.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_POLICY, nullSetting.code());
  }

  @Test
  void enforcesPolicyLimits() {
    SecurityPolicyModel model = new SecurityPolicyModel(new SecurityPolicyOptions(1));
    SecurityServiceException invalidLimit =
        assertThrows(SecurityServiceException.class, () -> new SecurityPolicyOptions(0));
    SecurityServiceException limit =
        assertThrows(
            SecurityServiceException.class,
            () ->
                model.validate(
                    List.of(
                        SecurityPolicySetting.authentication(
                            SecurityAuthenticationRequirement.REQUIRED),
                        SecurityPolicySetting.audit(SecurityAuditLevel.ALL))));

    assertEquals(SecurityServiceDiagnosticCodes.INVALID_LIMIT, invalidLimit.code());
    assertEquals(SecurityServiceDiagnosticCodes.POLICY_LIMIT_EXCEEDED, limit.code());
  }

  @Test
  void snapshotsAreImmutable() {
    SecurityPolicySnapshot snapshot =
        new SecurityPolicyModel()
            .validate(List.of(SecurityPolicySetting.audit(SecurityAuditLevel.ALL)));

    assertThrows(
        UnsupportedOperationException.class,
        () -> snapshot.settings().add(SecurityPolicySetting.audit(SecurityAuditLevel.OFF)));
  }

  @Test
  void publicSnapshotConstructorUsesCanonicalSettings() {
    SecurityPolicySnapshot snapshot =
        new SecurityPolicySnapshot(
            SecurityAuthenticationRequirement.REQUIRED,
            SecurityTrustRequirement.OPTIONAL,
            SecurityTransportProtection.INTEGRITY,
            SecurityIdentityAssertionMode.DISABLED,
            SecurityDelegationMode.DISABLED,
            SecurityAuditLevel.OFF,
            List.of(SecurityPolicySetting.audit(SecurityAuditLevel.ALL)));

    assertEquals(
        List.of(
            SecurityPolicySetting.authentication(SecurityAuthenticationRequirement.REQUIRED),
            SecurityPolicySetting.trust(SecurityTrustRequirement.OPTIONAL),
            SecurityPolicySetting.transport(SecurityTransportProtection.INTEGRITY),
            SecurityPolicySetting.identityAssertion(SecurityIdentityAssertionMode.DISABLED),
            SecurityPolicySetting.delegation(SecurityDelegationMode.DISABLED),
            SecurityPolicySetting.audit(SecurityAuditLevel.OFF)),
        snapshot.settings());
  }
}
