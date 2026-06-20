package io.github.mundanej.mjo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SecurityAuditDisclosureModelTest {

  private static final Instant NOW = Instant.parse("2026-06-20T13:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void createsAllowAuditEventWithDeterministicFields() {
    SecurityAuditEvent event =
        model()
            .event(
                SecurityPolicyEvaluationDecision.allow(),
                List.of(
                    new SecurityAuditField("target", "naming"),
                    new SecurityAuditField("operation", "resolve")));

    assertEquals(SecurityAuditEventType.POLICY_EVALUATION, event.type());
    assertEquals(NOW, event.occurredAt());
    assertEquals(SecurityPolicyEvaluationStatus.ALLOW, event.status());
    assertEquals(List.of(), event.failures());
    assertEquals(
        List.of("operation", "target"),
        event.fields().stream().map(SecurityAuditField::key).toList());
  }

  @Test
  void mapsDenialReasonsWithoutRawEvaluatorMessages() {
    SecurityPolicyEvaluationDecision decision =
        new SecurityPolicyEvaluationDecision(
            SecurityPolicyEvaluationStatus.DENY,
            List.of(
                new SecurityPolicyEvaluationReason(
                    SecurityServiceDiagnosticCodes.CREDENTIAL_UNTRUSTED,
                    "credential secret-token-123 is not trusted"),
                new SecurityPolicyEvaluationReason(
                    SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL,
                    "credential bearer-token is required")));

    SecurityAuditEvent event = model().event(decision, List.of());

    assertEquals(SecurityPolicyEvaluationStatus.DENY, event.status());
    assertEquals(
        List.of(
            SecurityServiceDiagnosticCodes.CREDENTIAL_UNTRUSTED,
            SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL),
        event.failures().stream().map(SecurityFailureDisclosure::code).toList());
    assertEquals(
        List.of("credential untrusted", "credential required"),
        event.failures().stream().map(SecurityFailureDisclosure::reason).toList());
    assertFalse(event.toString().contains("secret-token-123"));
    assertFalse(event.toString().contains("bearer-token"));
  }

  @Test
  void redactsSensitiveFieldsAndToStringOutput() {
    SecurityAuditEvent event =
        model()
            .event(
                SecurityPolicyEvaluationDecision.allow(),
                List.of(
                    new SecurityAuditField("credentialId", "credential-secret-123"),
                    new SecurityAuditField("note", "contains password value")));

    assertEquals(
        List.of(SecurityAuditRedaction.REDACTED, SecurityAuditRedaction.REDACTED),
        event.fields().stream().map(SecurityAuditField::value).toList());
    assertFalse(event.toString().contains("credential-secret-123"));
    assertFalse(event.toString().contains("password value"));
  }

  @Test
  void disclosesExceptionsWithoutCopyingExceptionMessages() {
    SecurityServiceException exception =
        new SecurityServiceException(
            SecurityServiceDiagnosticCodes.CREDENTIAL_UNTRUSTED,
            "credential secret-token-123 is not trusted");

    SecurityFailureDisclosure disclosure = model().disclose(exception);

    assertEquals(SecurityServiceDiagnosticCodes.CREDENTIAL_UNTRUSTED, disclosure.code());
    assertEquals("credential untrusted", disclosure.reason());
    assertFalse(disclosure.toString().contains("secret-token-123"));
  }

  @Test
  void rejectsMalformedAndOversizedAuditInputs() {
    SecurityAuditDisclosureModel smallModel =
        new SecurityAuditDisclosureModel(new SecurityAuditOptions(1, 1), FIXED_CLOCK);
    SecurityServiceException invalidOptions =
        assertThrows(SecurityServiceException.class, () -> new SecurityAuditOptions(-1, 1));
    SecurityServiceException blankField =
        assertThrows(SecurityServiceException.class, () -> new SecurityAuditField(" ", "value"));
    SecurityServiceException longKey =
        assertThrows(
            SecurityServiceException.class,
            () -> new SecurityAuditField("x".repeat(SecurityAuditField.MAX_KEY_LENGTH + 1), "v"));
    SecurityServiceException longValue =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityAuditField(
                    "field", "x".repeat(SecurityAuditField.MAX_VALUE_LENGTH + 1)));
    SecurityServiceException tooManyFields =
        assertThrows(
            SecurityServiceException.class,
            () ->
                smallModel.event(
                    SecurityPolicyEvaluationDecision.allow(),
                    List.of(new SecurityAuditField("a", "1"), new SecurityAuditField("b", "2"))));
    SecurityServiceException nullFields =
        assertThrows(
            SecurityServiceException.class,
            () -> smallModel.event(SecurityPolicyEvaluationDecision.allow(), null));
    SecurityServiceException nullField =
        assertThrows(
            SecurityServiceException.class,
            () ->
                smallModel.event(
                    SecurityPolicyEvaluationDecision.allow(), Collections.singletonList(null)));

    assertEquals(SecurityServiceDiagnosticCodes.INVALID_LIMIT, invalidOptions.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_AUDIT_EVENT, blankField.code());
    assertEquals(SecurityServiceDiagnosticCodes.AUDIT_EVENT_LIMIT_EXCEEDED, longKey.code());
    assertEquals(SecurityServiceDiagnosticCodes.AUDIT_EVENT_LIMIT_EXCEEDED, longValue.code());
    assertEquals(SecurityServiceDiagnosticCodes.AUDIT_EVENT_LIMIT_EXCEEDED, tooManyFields.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_AUDIT_EVENT, nullFields.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_AUDIT_EVENT, nullField.code());
  }

  @Test
  void rejectsMalformedDisclosureAndAuditEvents() {
    SecurityServiceException blankReason =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityFailureDisclosure(
                    SecurityServiceDiagnosticCodes.MALFORMED_POLICY, " "));
    SecurityServiceException longReason =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityFailureDisclosure(
                    SecurityServiceDiagnosticCodes.MALFORMED_POLICY,
                    "x".repeat(SecurityFailureDisclosure.MAX_REASON_LENGTH + 1)));
    SecurityFailureDisclosure missingCredential =
        new SecurityFailureDisclosure(
            SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL, "credential required");
    SecurityFailureDisclosure customCredentialDisclosure =
        new SecurityFailureDisclosure(
            SecurityServiceDiagnosticCodes.CREDENTIAL_UNTRUSTED,
            "credential secret-token-123 rejected");
    SecurityServiceException allowWithFailure =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityAuditEvent(
                    SecurityAuditEventType.POLICY_EVALUATION,
                    NOW,
                    SecurityPolicyEvaluationStatus.ALLOW,
                    List.of(missingCredential),
                    List.of()));
    SecurityServiceException denyWithoutFailure =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityAuditEvent(
                    SecurityAuditEventType.POLICY_EVALUATION,
                    NOW,
                    SecurityPolicyEvaluationStatus.DENY,
                    List.of(),
                    List.of()));
    SecurityServiceException directEventTooLarge =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityAuditEvent(
                    SecurityAuditEventType.POLICY_EVALUATION,
                    NOW,
                    SecurityPolicyEvaluationStatus.ALLOW,
                    List.of(),
                    Collections.nCopies(
                        SecurityAuditOptions.ABSOLUTE_MAX_FIELDS + 1,
                        new SecurityAuditField("operation", "resolve"))));

    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_AUDIT_EVENT, blankReason.code());
    assertEquals(SecurityServiceDiagnosticCodes.AUDIT_EVENT_LIMIT_EXCEEDED, longReason.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_AUDIT_EVENT, allowWithFailure.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_AUDIT_EVENT, denyWithoutFailure.code());
    assertEquals(
        SecurityServiceDiagnosticCodes.AUDIT_EVENT_LIMIT_EXCEEDED, directEventTooLarge.code());
    assertEquals(SecurityAuditRedaction.REDACTED, customCredentialDisclosure.reason());
    assertFalse(customCredentialDisclosure.toString().contains("secret-token-123"));
  }

  @Test
  void directAuditEventConstructorCanonicalizesReportOrdering() {
    SecurityFailureDisclosure untrusted =
        new SecurityFailureDisclosure(
            SecurityServiceDiagnosticCodes.CREDENTIAL_UNTRUSTED, "credential untrusted");
    SecurityFailureDisclosure missing =
        new SecurityFailureDisclosure(
            SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL, "credential required");

    SecurityAuditEvent event =
        new SecurityAuditEvent(
            SecurityAuditEventType.POLICY_EVALUATION,
            NOW,
            SecurityPolicyEvaluationStatus.DENY,
            List.of(missing, untrusted),
            List.of(
                new SecurityAuditField("target", "naming"),
                new SecurityAuditField("operation", "resolve")));

    assertEquals(
        List.of(
            SecurityServiceDiagnosticCodes.CREDENTIAL_UNTRUSTED,
            SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL),
        event.failures().stream().map(SecurityFailureDisclosure::code).toList());
    assertEquals(
        List.of("operation", "target"),
        event.fields().stream().map(SecurityAuditField::key).toList());
  }

  @Test
  void auditCollectionsAreImmutable() {
    SecurityAuditEvent event =
        model()
            .event(
                new SecurityPolicyEvaluationDecision(
                    SecurityPolicyEvaluationStatus.CHALLENGE,
                    List.of(
                        new SecurityPolicyEvaluationReason(
                            SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL,
                            "credential is required"))),
                List.of(new SecurityAuditField("target", "naming")));

    assertThrows(
        UnsupportedOperationException.class,
        () ->
            event
                .failures()
                .add(
                    new SecurityFailureDisclosure(
                        SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL, "credential required")));
    assertThrows(
        UnsupportedOperationException.class,
        () -> event.fields().add(new SecurityAuditField("operation", "resolve")));
  }

  private static SecurityAuditDisclosureModel model() {
    return new SecurityAuditDisclosureModel(SecurityAuditOptions.defaults(), FIXED_CLOCK);
  }
}
