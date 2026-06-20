package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.security.LocalSecurityTrustModel;
import io.github.mundanej.mjo.security.SecurityAuditDisclosureModel;
import io.github.mundanej.mjo.security.SecurityAuditEvent;
import io.github.mundanej.mjo.security.SecurityAuditField;
import io.github.mundanej.mjo.security.SecurityAuthenticationRequirement;
import io.github.mundanej.mjo.security.SecurityCredentialKind;
import io.github.mundanej.mjo.security.SecurityCsiv2IdentityTokenPolicy;
import io.github.mundanej.mjo.security.SecurityCsiv2Mechanism;
import io.github.mundanej.mjo.security.SecurityCsiv2MechanismIds;
import io.github.mundanej.mjo.security.SecurityCsiv2MetadataCodec;
import io.github.mundanej.mjo.security.SecurityCsiv2MetadataModel;
import io.github.mundanej.mjo.security.SecurityCsiv2MetadataSnapshot;
import io.github.mundanej.mjo.security.SecurityDelegationMode;
import io.github.mundanej.mjo.security.SecurityIiopBoundary;
import io.github.mundanej.mjo.security.SecurityIiopContextCodec;
import io.github.mundanej.mjo.security.SecurityIiopContextDescriptor;
import io.github.mundanej.mjo.security.SecurityPolicyEvaluationDecision;
import io.github.mundanej.mjo.security.SecurityPolicyEvaluationRequest;
import io.github.mundanej.mjo.security.SecurityPolicyEvaluationStatus;
import io.github.mundanej.mjo.security.SecurityPolicyEvaluator;
import io.github.mundanej.mjo.security.SecurityPolicyModel;
import io.github.mundanej.mjo.security.SecurityPolicySetting;
import io.github.mundanej.mjo.security.SecurityPolicySnapshot;
import io.github.mundanej.mjo.security.SecurityServiceDiagnosticCodes;
import io.github.mundanej.mjo.security.SecurityServiceException;
import io.github.mundanej.mjo.security.SecurityTransportProtection;
import io.github.mundanej.mjo.security.SecurityTrustEvaluationInput;
import io.github.mundanej.mjo.security.SecurityTrustRequirement;
import java.time.Instant;
import java.util.List;

/** Native Image smoke coverage for the supported local Security Service slice. */
public final class SecurityServiceNativeSmoke {

  private SecurityServiceNativeSmoke() {}

  /** Runs the Security Service Native Image smoke checks. */
  public static void main(String[] args) {
    LocalSecurityTrustModel trustModel = new LocalSecurityTrustModel();
    trustModel.registerTrustAnchor("anchor-a", "issuer-a", SecurityCredentialKind.USER);
    trustModel.registerCredential(
        "credential-a",
        "alice",
        SecurityCredentialKind.USER,
        "anchor-a",
        Instant.now().minusSeconds(60),
        Instant.now().plusSeconds(300));
    SecurityTrustEvaluationInput credential = trustModel.evaluationInput("credential-a");
    SmokeAssertions.requireEquals(
        "alice", trustModel.evaluate(credential).principalId().value(), "trusted credential");
    assertDiagnostic(
        SecurityServiceDiagnosticCodes.CREDENTIAL_UNTRUSTED,
        () ->
            new LocalSecurityTrustModel()
                .evaluate(SecurityTrustEvaluationInput.from(trustModel.listCredentials().get(0))),
        "untrusted credential rejection");

    SecurityPolicyModel policyModel = new SecurityPolicyModel();
    assertDiagnostic(
        SecurityServiceDiagnosticCodes.UNSUPPORTED_DELEGATION,
        () ->
            policyModel.validate(
                List.of(SecurityPolicySetting.delegation(SecurityDelegationMode.ENABLED))),
        "unsupported delegation policy");
    SecurityPolicySnapshot policy =
        policyModel.validate(
            List.of(
                SecurityPolicySetting.authentication(SecurityAuthenticationRequirement.REQUIRED),
                SecurityPolicySetting.trust(SecurityTrustRequirement.REQUIRED),
                SecurityPolicySetting.transport(SecurityTransportProtection.CONFIDENTIALITY)));

    SecurityCsiv2MetadataSnapshot metadata =
        new SecurityCsiv2MetadataModel()
            .validate(
                List.of(
                    new SecurityCsiv2Mechanism(
                        SecurityCsiv2MechanismIds.SUPPORTED_LOCAL,
                        SecurityTransportProtection.CONFIDENTIALITY,
                        SecurityCsiv2IdentityTokenPolicy.PRINCIPAL_NAME,
                        SecurityAuthenticationRequirement.REQUIRED,
                        SecurityAuthenticationRequirement.REQUIRED)));
    SecurityCsiv2MetadataCodec metadataCodec = new SecurityCsiv2MetadataCodec();
    SmokeAssertions.requireEquals(
        metadata, metadataCodec.decode(metadataCodec.encode(metadata)), "CSIv2 metadata codec");
    assertDiagnostic(
        SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA,
        () -> metadataCodec.decode("not-csiv2"),
        "malformed CSIv2 metadata");

    SecurityPolicyEvaluator evaluator = new SecurityPolicyEvaluator(trustModel);
    SecurityPolicyEvaluationDecision allow =
        evaluator.evaluate(new SecurityPolicyEvaluationRequest(policy, metadata, credential));
    SmokeAssertions.requireEquals(
        SecurityPolicyEvaluationStatus.ALLOW, allow.status(), "local policy allow");
    SecurityPolicyEvaluationDecision challenge =
        evaluator.evaluate(new SecurityPolicyEvaluationRequest(policy, metadata, null));
    SmokeAssertions.requireEquals(
        SecurityPolicyEvaluationStatus.CHALLENGE,
        challenge.status(),
        "missing credential challenge");

    SecurityAuditDisclosureModel auditModel = new SecurityAuditDisclosureModel();
    SecurityAuditEvent event =
        auditModel.event(
            challenge,
            List.of(
                new SecurityAuditField("credential", "super-secret-token"),
                new SecurityAuditField("principal", "alice")));
    SmokeAssertions.require(
        event.fields().stream().noneMatch(field -> field.value().contains("super-secret-token")),
        "audit redacts secrets");
    SmokeAssertions.require(
        event.failures().stream()
            .anyMatch(
                failure ->
                    failure.code().equals(SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL)
                        && failure.reason().equals("credential required")),
        "audit failure disclosure");

    SecurityIiopBoundary boundary =
        new SecurityIiopBoundary(evaluator, new SecurityIiopContextCodec(), auditModel);
    SecurityIiopContextDescriptor serviceContext = boundary.exportServiceContext(metadata);
    SmokeAssertions.requireEquals(
        SecurityPolicyEvaluationStatus.ALLOW,
        boundary.evaluateServiceContexts(policy, List.of(serviceContext), credential).status(),
        "IIOP service context allow");
    SecurityIiopContextDescriptor taggedComponent = boundary.exportTaggedComponent(metadata);
    SmokeAssertions.requireEquals(
        SecurityPolicyEvaluationStatus.ALLOW,
        boundary.evaluateTaggedComponents(policy, List.of(taggedComponent), credential).status(),
        "IIOP tagged component allow");
    assertDiagnostic(
        SecurityServiceDiagnosticCodes.MALFORMED_IIOP_SECURITY_CONTEXT,
        () ->
            boundary.evaluateServiceContexts(
                policy,
                List.of(
                    new SecurityIiopContextDescriptor(
                        serviceContext.kind(), serviceContext.contextId(), new byte[] {1})),
                credential),
        "malformed IIOP context");
    boundary.close();
    assertDiagnostic(
        SecurityServiceDiagnosticCodes.IIOP_SECURITY_BOUNDARY_CLOSED,
        () -> boundary.exportServiceContext(metadata),
        "IIOP boundary clean shutdown");
  }

  private static void assertDiagnostic(
      DiagnosticCode code, SmokeAssertions.ThrowingAction action, String label) {
    try {
      action.run();
    } catch (SecurityServiceException expected) {
      SmokeAssertions.requireEquals(code, expected.code(), label);
      return;
    } catch (Exception exception) {
      throw new AssertionError("Native Image smoke failed: " + label, exception);
    }
    throw new AssertionError("Native Image smoke failed: " + label + "; expected diagnostic");
  }
}
