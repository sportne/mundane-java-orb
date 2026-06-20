package io.github.mundanej.mjo.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class SecurityIiopBoundaryTest {

  private static final Instant NOW = Instant.parse("2026-06-20T12:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void encodesAndDecodesServiceContextAndTaggedComponent() {
    SecurityCsiv2MetadataSnapshot metadata = confidentialityMetadata();
    SecurityIiopBoundary boundary = boundaryWithTrust(new LocalSecurityTrustModel());
    SecurityIiopContextCodec codec = new SecurityIiopContextCodec();

    SecurityIiopContextDescriptor serviceContext = boundary.exportServiceContext(metadata);
    SecurityIiopContextDescriptor taggedComponent = boundary.exportTaggedComponent(metadata);

    assertEquals(SecurityIiopContextKind.SERVICE_CONTEXT, serviceContext.kind());
    assertEquals(
        SecurityIiopContextDescriptor.SECURITY_SERVICE_CONTEXT_ID, serviceContext.contextId());
    assertEquals(SecurityIiopContextKind.TAGGED_COMPONENT, taggedComponent.kind());
    assertEquals(
        SecurityIiopContextDescriptor.SECURITY_TAGGED_COMPONENT_ID, taggedComponent.contextId());
    assertEquals(metadata, codec.decode(serviceContext));
    assertEquals(metadata, codec.decode(taggedComponent));
    assertTrue(
        new String(serviceContext.contextData(), StandardCharsets.UTF_8)
            .startsWith(SecurityCsiv2MetadataCodec.VERSION));
  }

  @Test
  void descriptorCopiesContextBytesAndHidesRawPayload() {
    byte[] bytes = "context".getBytes(StandardCharsets.UTF_8);
    SecurityIiopContextDescriptor descriptor =
        new SecurityIiopContextDescriptor(
            SecurityIiopContextKind.SERVICE_CONTEXT,
            SecurityIiopContextDescriptor.SECURITY_SERVICE_CONTEXT_ID,
            bytes);
    SecurityIiopContextDescriptor same =
        new SecurityIiopContextDescriptor(
            SecurityIiopContextKind.SERVICE_CONTEXT,
            SecurityIiopContextDescriptor.SECURITY_SERVICE_CONTEXT_ID,
            "context".getBytes(StandardCharsets.UTF_8));

    bytes[0] = 'x';
    byte[] exported = descriptor.contextData();
    exported[0] = 'y';

    assertArrayEquals("context".getBytes(StandardCharsets.UTF_8), descriptor.contextData());
    assertEquals(descriptor, same);
    assertEquals(descriptor.hashCode(), same.hashCode());
    assertNotEquals(
        descriptor,
        new SecurityIiopContextDescriptor(
            SecurityIiopContextKind.TAGGED_COMPONENT,
            SecurityIiopContextDescriptor.SECURITY_TAGGED_COMPONENT_ID,
            "context".getBytes(StandardCharsets.UTF_8)));
    assertEquals(
        "SecurityIiopContextDescriptor[kind=SERVICE_CONTEXT, contextId=1296716611, contextDataLength=7]",
        descriptor.toString());
  }

  @Test
  void findsBoundedSecurityContextsAndIgnoresUnrelatedDescriptors() {
    SecurityIiopContextCodec codec = new SecurityIiopContextCodec();
    SecurityIiopContextDescriptor unrelated =
        new SecurityIiopContextDescriptor(
            SecurityIiopContextKind.SERVICE_CONTEXT, 7, new byte[] {1});
    SecurityIiopContextDescriptor serviceContext =
        codec.encodeServiceContext(new SecurityCsiv2MetadataModel().defaults());

    Optional<SecurityIiopContextDescriptor> found =
        codec.findServiceContext(List.of(unrelated, serviceContext));
    Optional<SecurityIiopContextDescriptor> absent = codec.findTaggedComponent(List.of(unrelated));

    assertTrue(found.isPresent());
    assertEquals(serviceContext, found.orElseThrow());
    assertTrue(absent.isEmpty());
  }

  @Test
  void rejectsMalformedDuplicateAndOversizedContexts() {
    SecurityIiopContextCodec codec = new SecurityIiopContextCodec();
    SecurityIiopContextDescriptor malformedUtf8 =
        new SecurityIiopContextDescriptor(
            SecurityIiopContextKind.SERVICE_CONTEXT,
            SecurityIiopContextDescriptor.SECURITY_SERVICE_CONTEXT_ID,
            new byte[] {(byte) 0xC3, 0x28});
    SecurityIiopContextDescriptor wrongId =
        new SecurityIiopContextDescriptor(
            SecurityIiopContextKind.SERVICE_CONTEXT, 7, "context".getBytes(StandardCharsets.UTF_8));
    SecurityIiopContextDescriptor serviceContext =
        codec.encodeServiceContext(new SecurityCsiv2MetadataModel().defaults());

    SecurityServiceException malformed =
        assertThrows(SecurityServiceException.class, () -> codec.decode(malformedUtf8));
    SecurityServiceException wrong =
        assertThrows(SecurityServiceException.class, () -> codec.decode(wrongId));
    SecurityServiceException duplicate =
        assertThrows(
            SecurityServiceException.class,
            () -> codec.findServiceContext(List.of(serviceContext, serviceContext)));
    SecurityServiceException empty =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityIiopContextDescriptor(
                    SecurityIiopContextKind.SERVICE_CONTEXT,
                    SecurityIiopContextDescriptor.SECURITY_SERVICE_CONTEXT_ID,
                    new byte[0]));
    SecurityServiceException oversized =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityIiopContextDescriptor(
                    SecurityIiopContextKind.SERVICE_CONTEXT,
                    SecurityIiopContextDescriptor.SECURITY_SERVICE_CONTEXT_ID,
                    new byte[SecurityCsiv2MetadataOptions.ABSOLUTE_MAX_ENCODED_LENGTH + 1]));
    SecurityServiceException tooMany =
        assertThrows(
            SecurityServiceException.class,
            () ->
                codec.findServiceContext(
                    Collections.nCopies(
                        SecurityIiopContextCodec.MAX_CONTEXT_DESCRIPTORS + 1, wrongId)));
    SecurityServiceException nullKind =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityIiopContextDescriptor(
                    null,
                    SecurityIiopContextDescriptor.SECURITY_SERVICE_CONTEXT_ID,
                    new byte[] {1}));
    SecurityServiceException nullData =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityIiopContextDescriptor(
                    SecurityIiopContextKind.SERVICE_CONTEXT,
                    SecurityIiopContextDescriptor.SECURITY_SERVICE_CONTEXT_ID,
                    null));
    SecurityServiceException nullList =
        assertThrows(SecurityServiceException.class, () -> codec.findServiceContext(null));
    SecurityServiceException nullEntry =
        assertThrows(
            SecurityServiceException.class,
            () -> codec.findServiceContext(Collections.singletonList(null)));

    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_IIOP_SECURITY_CONTEXT, malformed.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_IIOP_SECURITY_CONTEXT, wrong.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_IIOP_SECURITY_CONTEXT, duplicate.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_IIOP_SECURITY_CONTEXT, empty.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_IIOP_SECURITY_CONTEXT, oversized.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_IIOP_SECURITY_CONTEXT, tooMany.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_IIOP_SECURITY_CONTEXT, nullKind.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_IIOP_SECURITY_CONTEXT, nullData.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_IIOP_SECURITY_CONTEXT, nullList.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_IIOP_SECURITY_CONTEXT, nullEntry.code());
  }

  @Test
  void evaluatesAllowDenyAndChallengeThroughLoopbackBoundary() {
    LocalSecurityTrustModel trustModel = trustModelWithCredential();
    SecurityIiopBoundary boundary = boundaryWithTrust(trustModel);
    SecurityIiopContextDescriptor defaultContext =
        boundary.exportServiceContext(new SecurityCsiv2MetadataModel().defaults());
    SecurityIiopContextDescriptor confidentialContext =
        boundary.exportServiceContext(confidentialityMetadata());
    SecurityPolicySnapshot optionalPolicy = new SecurityPolicyModel().defaults();
    SecurityPolicySnapshot requiredPolicy =
        new SecurityPolicyModel()
            .validate(
                List.of(
                    SecurityPolicySetting.authentication(
                        SecurityAuthenticationRequirement.REQUIRED)));
    SecurityPolicySnapshot confidentialPolicy =
        new SecurityPolicyModel()
            .validate(
                List.of(
                    SecurityPolicySetting.transport(SecurityTransportProtection.CONFIDENTIALITY)));

    SecurityPolicyEvaluationDecision allow =
        boundary.evaluateServiceContexts(optionalPolicy, List.of(defaultContext), null);
    SecurityPolicyEvaluationDecision challenge =
        boundary.evaluateServiceContexts(requiredPolicy, List.of(confidentialContext), null);
    SecurityPolicyEvaluationDecision deny =
        boundary.evaluateServiceContexts(confidentialPolicy, List.of(defaultContext), null);

    assertEquals(SecurityPolicyEvaluationStatus.ALLOW, allow.status());
    assertEquals(SecurityPolicyEvaluationStatus.CHALLENGE, challenge.status());
    assertEquals(List.of(SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL), codes(challenge));
    assertEquals(SecurityPolicyEvaluationStatus.DENY, deny.status());
    assertEquals(
        List.of(SecurityServiceDiagnosticCodes.INSUFFICIENT_TRANSPORT_PROTECTION), codes(deny));
  }

  @Test
  void evaluatesTaggedComponentWithTrustedCredential() {
    LocalSecurityTrustModel trustModel = trustModelWithCredential();
    SecurityIiopBoundary boundary = boundaryWithTrust(trustModel);
    SecurityIiopContextDescriptor taggedComponent =
        boundary.exportTaggedComponent(confidentialityMetadata());
    SecurityPolicySnapshot policy =
        new SecurityPolicyModel()
            .validate(
                List.of(
                    SecurityPolicySetting.authentication(
                        SecurityAuthenticationRequirement.REQUIRED),
                    SecurityPolicySetting.trust(SecurityTrustRequirement.REQUIRED)));

    SecurityPolicyEvaluationDecision decision =
        boundary.evaluateTaggedComponents(
            policy, List.of(taggedComponent), trustModel.evaluationInput("credential-a"));

    assertEquals(SecurityPolicyEvaluationStatus.ALLOW, decision.status());
    assertEquals(List.of(), decision.reasons());
  }

  @Test
  void createsRedactedAuditEventForIiopDecisions() {
    SecurityIiopBoundary boundary = boundaryWithTrust(new LocalSecurityTrustModel());
    SecurityIiopContextDescriptor descriptor =
        boundary.exportServiceContext(new SecurityCsiv2MetadataModel().defaults());

    SecurityAuditEvent event =
        boundary.auditServiceContexts(
            new SecurityPolicyModel().defaults(),
            List.of(descriptor),
            null,
            List.of(new SecurityAuditField("credentialId", "credential-secret-123")));

    assertEquals(SecurityPolicyEvaluationStatus.ALLOW, event.status());
    assertEquals(SecurityAuditRedaction.REDACTED, event.fields().getFirst().value());
    assertFalse(event.toString().contains("credential-secret-123"));
  }

  @Test
  void rejectsBoundaryUseAfterCleanShutdown() {
    SecurityIiopBoundary boundary = boundaryWithTrust(new LocalSecurityTrustModel());
    SecurityIiopContextDescriptor descriptor =
        boundary.exportServiceContext(new SecurityCsiv2MetadataModel().defaults());

    boundary.close();
    boundary.close();

    SecurityServiceException exportAfterClose =
        assertThrows(
            SecurityServiceException.class,
            () -> boundary.exportServiceContext(new SecurityCsiv2MetadataModel().defaults()));
    SecurityServiceException evaluateAfterClose =
        assertThrows(
            SecurityServiceException.class,
            () ->
                boundary.evaluateServiceContexts(
                    new SecurityPolicyModel().defaults(), List.of(descriptor), null));

    assertTrue(boundary.isClosed());
    assertEquals(
        SecurityServiceDiagnosticCodes.IIOP_SECURITY_BOUNDARY_CLOSED, exportAfterClose.code());
    assertEquals(
        SecurityServiceDiagnosticCodes.IIOP_SECURITY_BOUNDARY_CLOSED, evaluateAfterClose.code());
  }

  @Test
  void iiopBoundaryTypesAvoidForbiddenRuntimeMechanisms() {
    assertFalse(Serializable.class.isAssignableFrom(SecurityIiopBoundary.class));
    assertFalse(Serializable.class.isAssignableFrom(SecurityIiopContextCodec.class));
    assertFalse(Serializable.class.isAssignableFrom(SecurityIiopContextDescriptor.class));
    assertTrue(Modifier.isFinal(SecurityIiopBoundary.class.getModifiers()));
    assertTrue(Modifier.isFinal(SecurityIiopContextCodec.class.getModifiers()));
    assertTrue(Modifier.isFinal(SecurityIiopContextDescriptor.class.getModifiers()));
  }

  private static SecurityIiopBoundary boundaryWithTrust(LocalSecurityTrustModel trustModel) {
    return new SecurityIiopBoundary(new SecurityPolicyEvaluator(trustModel, FIXED_CLOCK));
  }

  private static LocalSecurityTrustModel trustModelWithCredential() {
    LocalSecurityTrustModel model =
        new LocalSecurityTrustModel(SecurityServiceOptions.defaults(), FIXED_CLOCK);
    model.registerTrustAnchor("anchor-a", "issuer-a", SecurityCredentialKind.USER);
    model.registerCredential(
        "credential-a",
        "principal-a",
        SecurityCredentialKind.USER,
        "anchor-a",
        NOW.minusSeconds(60),
        NOW.plusSeconds(60));
    return model;
  }

  private static SecurityCsiv2MetadataSnapshot confidentialityMetadata() {
    return new SecurityCsiv2MetadataModel()
        .validate(
            List.of(
                new SecurityCsiv2Mechanism(
                    SecurityCsiv2MechanismIds.SUPPORTED_LOCAL,
                    SecurityTransportProtection.CONFIDENTIALITY,
                    SecurityCsiv2IdentityTokenPolicy.ABSENT,
                    SecurityAuthenticationRequirement.OPTIONAL,
                    SecurityAuthenticationRequirement.OPTIONAL)));
  }

  private static List<DiagnosticCode> codes(SecurityPolicyEvaluationDecision decision) {
    return decision.reasons().stream().map(SecurityPolicyEvaluationReason::code).toList();
  }
}
