package io.github.mundanej.mjo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SecurityPolicyEvaluatorTest {

  private static final Instant NOW = Instant.parse("2026-06-20T12:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void allowsOptionalPolicyWithoutCredential() {
    SecurityPolicyEvaluationDecision decision =
        new SecurityPolicyEvaluator(new LocalSecurityTrustModel(), FIXED_CLOCK)
            .evaluate(
                SecurityPolicyEvaluationRequest.anonymous(
                    new SecurityPolicyModel().defaults(),
                    new SecurityCsiv2MetadataModel().defaults()));

    assertEquals(SecurityPolicyEvaluationStatus.ALLOW, decision.status());
    assertEquals(List.of(), decision.reasons());
  }

  @Test
  void allowsTrustedCredentialWhenPolicyRequiresAuthenticationAndTrust() {
    LocalSecurityTrustModel trustModel = trustModelWithCredential();
    SecurityPolicySnapshot policy =
        new SecurityPolicyModel()
            .validate(
                List.of(
                    SecurityPolicySetting.authentication(
                        SecurityAuthenticationRequirement.REQUIRED),
                    SecurityPolicySetting.trust(SecurityTrustRequirement.REQUIRED)));

    SecurityPolicyEvaluationDecision decision =
        new SecurityPolicyEvaluator(trustModel, FIXED_CLOCK)
            .evaluate(
                SecurityPolicyEvaluationRequest.authenticated(
                    policy,
                    new SecurityCsiv2MetadataModel().defaults(),
                    trustModel.evaluationInput("credential-a")));

    assertEquals(SecurityPolicyEvaluationStatus.ALLOW, decision.status());
    assertEquals(List.of(), decision.reasons());
  }

  @Test
  void challengesWhenRequiredCredentialIsMissing() {
    SecurityPolicySnapshot policy =
        new SecurityPolicyModel()
            .validate(
                List.of(
                    SecurityPolicySetting.authentication(
                        SecurityAuthenticationRequirement.REQUIRED)));

    SecurityPolicyEvaluationDecision decision =
        new SecurityPolicyEvaluator(new LocalSecurityTrustModel(), FIXED_CLOCK)
            .evaluate(
                SecurityPolicyEvaluationRequest.anonymous(
                    policy, new SecurityCsiv2MetadataModel().defaults()));

    assertEquals(SecurityPolicyEvaluationStatus.CHALLENGE, decision.status());
    assertEquals(List.of(SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL), codes(decision));
  }

  @Test
  void deniesExpiredAndUntrustedCredentials() {
    LocalSecurityTrustModel expiredModel =
        new LocalSecurityTrustModel(SecurityServiceOptions.defaults(), FIXED_CLOCK);
    expiredModel.registerTrustAnchor("anchor-a", "issuer-a", SecurityCredentialKind.USER);
    expiredModel.registerCredential(
        "credential-a",
        "principal-a",
        SecurityCredentialKind.USER,
        "anchor-a",
        NOW.minusSeconds(300),
        NOW.minusSeconds(1));
    LocalSecurityTrustModel untrustedModel =
        new LocalSecurityTrustModel(SecurityServiceOptions.defaults(), FIXED_CLOCK);
    untrustedModel.registerTrustAnchor("anchor-a", "issuer-a", SecurityCredentialKind.SERVICE);
    SecurityTrustEvaluationInput untrusted =
        new SecurityTrustEvaluationInput(
            new SecurityCredentialId("credential-b"),
            new PrincipalId("principal-b"),
            SecurityCredentialKind.USER,
            new SecurityTrustAnchorId("anchor-a"),
            NOW.minusSeconds(60),
            NOW.plusSeconds(60));
    SecurityPolicySnapshot policy =
        new SecurityPolicyModel()
            .validate(
                List.of(
                    SecurityPolicySetting.authentication(
                        SecurityAuthenticationRequirement.REQUIRED),
                    SecurityPolicySetting.trust(SecurityTrustRequirement.REQUIRED)));

    SecurityPolicyEvaluationDecision expiredDecision =
        new SecurityPolicyEvaluator(expiredModel, FIXED_CLOCK)
            .evaluate(
                SecurityPolicyEvaluationRequest.authenticated(
                    policy,
                    new SecurityCsiv2MetadataModel().defaults(),
                    expiredModel.evaluationInput("credential-a")));
    SecurityPolicyEvaluationDecision untrustedDecision =
        new SecurityPolicyEvaluator(untrustedModel, FIXED_CLOCK)
            .evaluate(
                SecurityPolicyEvaluationRequest.authenticated(
                    policy, new SecurityCsiv2MetadataModel().defaults(), untrusted));

    assertEquals(SecurityPolicyEvaluationStatus.DENY, expiredDecision.status());
    assertEquals(
        List.of(SecurityServiceDiagnosticCodes.CREDENTIAL_EXPIRED), codes(expiredDecision));
    assertEquals(SecurityPolicyEvaluationStatus.DENY, untrustedDecision.status());
    assertEquals(
        List.of(SecurityServiceDiagnosticCodes.CREDENTIAL_UNTRUSTED), codes(untrustedDecision));
  }

  @Test
  void deniesInsufficientTransportProtection() {
    SecurityPolicySnapshot policy =
        new SecurityPolicyModel()
            .validate(
                List.of(
                    SecurityPolicySetting.transport(SecurityTransportProtection.CONFIDENTIALITY)));

    SecurityPolicyEvaluationDecision decision =
        new SecurityPolicyEvaluator(
                new LocalSecurityTrustModel(SecurityServiceOptions.defaults(), FIXED_CLOCK),
                FIXED_CLOCK)
            .evaluate(
                SecurityPolicyEvaluationRequest.anonymous(
                    policy, new SecurityCsiv2MetadataModel().defaults()));

    assertEquals(SecurityPolicyEvaluationStatus.DENY, decision.status());
    assertEquals(
        List.of(SecurityServiceDiagnosticCodes.INSUFFICIENT_TRANSPORT_PROTECTION), codes(decision));
  }

  @Test
  void deniesMalformedMetadataAndUnsupportedDelegation() {
    SecurityPolicySnapshot unsupportedDelegation =
        new SecurityPolicySnapshot(
            SecurityAuthenticationRequirement.OPTIONAL,
            SecurityTrustRequirement.OPTIONAL,
            SecurityTransportProtection.NONE,
            SecurityIdentityAssertionMode.DISABLED,
            SecurityDelegationMode.ENABLED,
            SecurityAuditLevel.OFF,
            List.of());

    SecurityPolicyEvaluationDecision decision =
        new SecurityPolicyEvaluator(new LocalSecurityTrustModel(), FIXED_CLOCK)
            .evaluate(SecurityPolicyEvaluationRequest.anonymous(unsupportedDelegation, null));

    assertEquals(SecurityPolicyEvaluationStatus.DENY, decision.status());
    assertEquals(
        List.of(
            SecurityServiceDiagnosticCodes.UNSUPPORTED_DELEGATION,
            SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA),
        codes(decision));
  }

  @Test
  void deniesPolicyConflictsAndKeepsReasonOrderingStable() {
    SecurityPolicySnapshot conflicting =
        new SecurityPolicySnapshot(
            SecurityAuthenticationRequirement.OPTIONAL,
            SecurityTrustRequirement.REQUIRED,
            SecurityTransportProtection.CONFIDENTIALITY,
            SecurityIdentityAssertionMode.SELF,
            SecurityDelegationMode.ENABLED,
            SecurityAuditLevel.OFF,
            List.of());

    SecurityPolicyEvaluationDecision decision =
        new SecurityPolicyEvaluator(new LocalSecurityTrustModel(), FIXED_CLOCK)
            .evaluate(SecurityPolicyEvaluationRequest.anonymous(conflicting, null));

    assertEquals(SecurityPolicyEvaluationStatus.DENY, decision.status());
    assertEquals(
        List.of(
            SecurityServiceDiagnosticCodes.UNSUPPORTED_DELEGATION,
            SecurityServiceDiagnosticCodes.CONFLICTING_POLICY,
            SecurityServiceDiagnosticCodes.CONFLICTING_POLICY,
            SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA,
            SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL),
        codes(decision));
  }

  @Test
  void rejectsMutableDecisionReasons() {
    SecurityPolicyEvaluationDecision decision =
        new SecurityPolicyEvaluationDecision(
            SecurityPolicyEvaluationStatus.DENY,
            List.of(
                new SecurityPolicyEvaluationReason(
                    SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL, "credential is required")));

    assertThrows(
        UnsupportedOperationException.class,
        () ->
            decision
                .reasons()
                .add(
                    new SecurityPolicyEvaluationReason(
                        SecurityServiceDiagnosticCodes.MALFORMED_POLICY, "policy is missing")));
  }

  @Test
  void rejectsInconsistentDecisionConstruction() {
    SecurityPolicyEvaluationReason missingCredential =
        new SecurityPolicyEvaluationReason(
            SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL, "credential is required");
    SecurityPolicyEvaluationReason malformedPolicy =
        new SecurityPolicyEvaluationReason(
            SecurityServiceDiagnosticCodes.MALFORMED_POLICY, "policy is missing");

    SecurityServiceException allowWithReason =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityPolicyEvaluationDecision(
                    SecurityPolicyEvaluationStatus.ALLOW, List.of(missingCredential)));
    SecurityServiceException challengeWithWrongReason =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityPolicyEvaluationDecision(
                    SecurityPolicyEvaluationStatus.CHALLENGE, List.of(malformedPolicy)));
    SecurityServiceException denyWithoutReason =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityPolicyEvaluationDecision(
                    SecurityPolicyEvaluationStatus.DENY, List.of()));

    assertEquals(
        SecurityServiceDiagnosticCodes.MALFORMED_POLICY_EVALUATION, allowWithReason.code());
    assertEquals(
        SecurityServiceDiagnosticCodes.MALFORMED_POLICY_EVALUATION,
        challengeWithWrongReason.code());
    assertEquals(
        SecurityServiceDiagnosticCodes.MALFORMED_POLICY_EVALUATION, denyWithoutReason.code());
  }

  @Test
  void rejectsOversizedEvaluationReasonMessages() {
    SecurityServiceException oversized =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityPolicyEvaluationReason(
                    SecurityServiceDiagnosticCodes.MALFORMED_POLICY,
                    "x".repeat(SecurityPolicyEvaluationReason.MAX_MESSAGE_LENGTH + 1)));

    assertEquals(SecurityServiceDiagnosticCodes.POLICY_EVALUATION_LIMIT_EXCEEDED, oversized.code());
  }

  private static LocalSecurityTrustModel trustModelWithCredential() {
    LocalSecurityTrustModel trustModel =
        new LocalSecurityTrustModel(SecurityServiceOptions.defaults(), FIXED_CLOCK);
    trustModel.registerTrustAnchor("anchor-a", "issuer-a", SecurityCredentialKind.USER);
    trustModel.registerCredential(
        "credential-a",
        "principal-a",
        SecurityCredentialKind.USER,
        "anchor-a",
        NOW.minusSeconds(60),
        NOW.plusSeconds(60));
    return trustModel;
  }

  private static List<DiagnosticCode> codes(SecurityPolicyEvaluationDecision decision) {
    return decision.reasons().stream().map(SecurityPolicyEvaluationReason::code).toList();
  }
}
