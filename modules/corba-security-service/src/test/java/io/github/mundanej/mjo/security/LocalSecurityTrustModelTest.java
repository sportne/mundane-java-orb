package io.github.mundanej.mjo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

final class LocalSecurityTrustModelTest {

  private static final Instant ISSUED_AT = Instant.parse("2026-06-20T12:00:00Z");
  private static final Instant EXPIRES_AT = Instant.parse("2026-06-20T13:00:00Z");

  @Test
  void registersTrustAnchorsAndCredentialsInDeterministicOrder() {
    LocalSecurityTrustModel model = new LocalSecurityTrustModel();

    model.registerTrustAnchor("anchor-a", "issuer-a", SecurityCredentialKind.USER);
    model.registerTrustAnchor("anchor-b", "issuer-b", SecurityCredentialKind.SERVICE);
    SecurityCredentialSnapshot credential =
        model.registerCredential(
            "credential-a",
            "principal-a",
            SecurityCredentialKind.USER,
            "anchor-a",
            ISSUED_AT,
            EXPIRES_AT);
    model.registerCredential(
        "credential-b",
        "principal-b",
        SecurityCredentialKind.SERVICE,
        "anchor-b",
        ISSUED_AT,
        EXPIRES_AT);

    assertEquals("credential-a", credential.credentialId().value());
    assertEquals(
        List.of(new SecurityCredentialId("credential-a"), new SecurityCredentialId("credential-b")),
        model.listCredentials().stream().map(SecurityCredentialSnapshot::credentialId).toList());
    assertEquals(
        List.of(new SecurityTrustAnchorId("anchor-a"), new SecurityTrustAnchorId("anchor-b")),
        model.listTrustAnchors().stream().map(SecurityTrustAnchorSnapshot::trustAnchorId).toList());
    assertTrue(model.lookupCredential("credential-a").isPresent());
    assertTrue(model.lookupTrustAnchor("anchor-a").isPresent());
  }

  @Test
  void rejectsDuplicateMissingAndMalformedIdentifiers() {
    LocalSecurityTrustModel model = new LocalSecurityTrustModel();
    model.registerTrustAnchor("anchor-a", "issuer-a", SecurityCredentialKind.USER);
    model.registerCredential(
        "credential-a",
        "principal-a",
        SecurityCredentialKind.USER,
        "anchor-a",
        ISSUED_AT,
        EXPIRES_AT);

    SecurityServiceException duplicateAnchor =
        assertThrows(
            SecurityServiceException.class,
            () -> model.registerTrustAnchor("anchor-a", "issuer-b", SecurityCredentialKind.USER));
    SecurityServiceException duplicateCredential =
        assertThrows(
            SecurityServiceException.class,
            () ->
                model.registerCredential(
                    "credential-a",
                    "principal-b",
                    SecurityCredentialKind.USER,
                    "anchor-a",
                    ISSUED_AT,
                    EXPIRES_AT));
    SecurityServiceException missingAnchor =
        assertThrows(SecurityServiceException.class, () -> model.removeTrustAnchor("missing"));
    SecurityServiceException missingCredential =
        assertThrows(SecurityServiceException.class, () -> model.removeCredential("missing"));
    SecurityServiceException malformed =
        assertThrows(
            SecurityServiceException.class,
            () ->
                model.registerCredential(
                    " ",
                    "principal-b",
                    SecurityCredentialKind.USER,
                    "anchor-a",
                    ISSUED_AT,
                    EXPIRES_AT));

    assertEquals(
        SecurityServiceDiagnosticCodes.TRUST_ANCHOR_ALREADY_EXISTS, duplicateAnchor.code());
    assertEquals(
        SecurityServiceDiagnosticCodes.CREDENTIAL_ALREADY_EXISTS, duplicateCredential.code());
    assertEquals(SecurityServiceDiagnosticCodes.TRUST_ANCHOR_NOT_FOUND, missingAnchor.code());
    assertEquals(SecurityServiceDiagnosticCodes.CREDENTIAL_NOT_FOUND, missingCredential.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_IDENTIFIER, malformed.code());
  }

  @Test
  void enforcesConfiguredLimits() {
    LocalSecurityTrustModel model =
        new LocalSecurityTrustModel(new SecurityServiceOptions(1, 1, 16));
    model.registerTrustAnchor("anchor-a", "issuer-a", SecurityCredentialKind.USER);
    model.registerCredential(
        "credential-a",
        "principal-a",
        SecurityCredentialKind.USER,
        "anchor-a",
        ISSUED_AT,
        EXPIRES_AT);

    SecurityServiceException credentialLimit =
        assertThrows(
            SecurityServiceException.class,
            () ->
                model.registerCredential(
                    "credential-b",
                    "principal-b",
                    SecurityCredentialKind.USER,
                    "anchor-a",
                    ISSUED_AT,
                    EXPIRES_AT));
    SecurityServiceException anchorLimit =
        assertThrows(
            SecurityServiceException.class,
            () -> model.registerTrustAnchor("anchor-b", "issuer-b", SecurityCredentialKind.USER));
    SecurityServiceException longIdentifier =
        assertThrows(
            SecurityServiceException.class,
            () -> model.lookupCredential("credential-id-is-too-long"));
    SecurityServiceException invalidLimit =
        assertThrows(SecurityServiceException.class, () -> new SecurityServiceOptions(0, 1, 1));

    assertEquals(SecurityServiceDiagnosticCodes.CREDENTIAL_LIMIT_EXCEEDED, credentialLimit.code());
    assertEquals(SecurityServiceDiagnosticCodes.TRUST_ANCHOR_LIMIT_EXCEEDED, anchorLimit.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_IDENTIFIER, longIdentifier.code());
    assertEquals(SecurityServiceDiagnosticCodes.INVALID_LIMIT, invalidLimit.code());
  }

  @Test
  void validatesCredentialLifetimeMetadata() {
    LocalSecurityTrustModel model = new LocalSecurityTrustModel();

    SecurityServiceException malformedLifetime =
        assertThrows(
            SecurityServiceException.class,
            () ->
                model.registerCredential(
                    "credential-a",
                    "principal-a",
                    SecurityCredentialKind.USER,
                    "anchor-a",
                    EXPIRES_AT,
                    ISSUED_AT));
    SecurityServiceException nullLifetime =
        assertThrows(
            SecurityServiceException.class,
            () ->
                model.registerCredential(
                    "credential-b",
                    "principal-a",
                    SecurityCredentialKind.USER,
                    "anchor-a",
                    null,
                    EXPIRES_AT));
    SecurityServiceException nullKind =
        assertThrows(
            SecurityServiceException.class,
            () ->
                model.registerCredential(
                    "credential-c", "principal-a", null, "anchor-a", ISSUED_AT, EXPIRES_AT));

    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_LIFETIME, malformedLifetime.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_LIFETIME, nullLifetime.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_IDENTIFIER, nullKind.code());
  }

  @Test
  void evaluatesTrustedCredentialsWithDeterministicDecision() {
    Instant now = Instant.parse("2026-06-20T12:30:00Z");
    LocalSecurityTrustModel model =
        new LocalSecurityTrustModel(
            SecurityServiceOptions.defaults(), Clock.fixed(now, ZoneOffset.UTC));
    model.registerTrustAnchor("anchor-a", "issuer-a", SecurityCredentialKind.USER);
    model.registerCredential(
        "credential-a",
        "principal-a",
        SecurityCredentialKind.USER,
        "anchor-a",
        ISSUED_AT,
        EXPIRES_AT);

    SecurityTrustDecision decision = model.evaluateCredential("credential-a");

    assertEquals(new SecurityCredentialId("credential-a"), decision.credentialId());
    assertEquals(new PrincipalId("principal-a"), decision.principalId());
    assertEquals(new SecurityTrustAnchorId("anchor-a"), decision.trustAnchorId());
    assertEquals(now, decision.evaluatedAt());
  }

  @Test
  void rejectsExpiredAndUntrustedCredentials() {
    LocalSecurityTrustModel expiredModel =
        new LocalSecurityTrustModel(
            SecurityServiceOptions.defaults(),
            Clock.fixed(Instant.parse("2026-06-20T13:00:00Z"), ZoneOffset.UTC));
    expiredModel.registerTrustAnchor("anchor-a", "issuer-a", SecurityCredentialKind.USER);
    expiredModel.registerCredential(
        "credential-a",
        "principal-a",
        SecurityCredentialKind.USER,
        "anchor-a",
        ISSUED_AT,
        EXPIRES_AT);

    LocalSecurityTrustModel untrustedModel =
        new LocalSecurityTrustModel(
            SecurityServiceOptions.defaults(),
            Clock.fixed(Instant.parse("2026-06-20T12:30:00Z"), ZoneOffset.UTC));
    untrustedModel.registerCredential(
        "credential-a",
        "principal-a",
        SecurityCredentialKind.USER,
        "unknown-anchor",
        ISSUED_AT,
        EXPIRES_AT);

    SecurityServiceException expired =
        assertThrows(
            SecurityServiceException.class, () -> expiredModel.evaluateCredential("credential-a"));
    SecurityServiceException untrusted =
        assertThrows(
            SecurityServiceException.class,
            () -> untrustedModel.evaluateCredential("credential-a"));

    assertEquals(SecurityServiceDiagnosticCodes.CREDENTIAL_EXPIRED, expired.code());
    assertEquals(SecurityServiceDiagnosticCodes.CREDENTIAL_UNTRUSTED, untrusted.code());
  }

  @Test
  void rejectsWrongCredentialKindForTrustAnchor() {
    LocalSecurityTrustModel model =
        new LocalSecurityTrustModel(
            SecurityServiceOptions.defaults(),
            Clock.fixed(Instant.parse("2026-06-20T12:30:00Z"), ZoneOffset.UTC));
    model.registerTrustAnchor("anchor-a", "issuer-a", SecurityCredentialKind.SERVICE);
    model.registerCredential(
        "credential-a",
        "principal-a",
        SecurityCredentialKind.USER,
        "anchor-a",
        ISSUED_AT,
        EXPIRES_AT);

    SecurityServiceException untrusted =
        assertThrows(
            SecurityServiceException.class, () -> model.evaluateCredential("credential-a"));

    assertEquals(SecurityServiceDiagnosticCodes.CREDENTIAL_UNTRUSTED, untrusted.code());
  }

  @Test
  void createsEvaluationInputAndRejectsMissingCredentials() {
    LocalSecurityTrustModel model = new LocalSecurityTrustModel();
    model.registerCredential(
        "credential-a",
        "principal-a",
        SecurityCredentialKind.USER,
        "anchor-a",
        ISSUED_AT,
        EXPIRES_AT);

    SecurityTrustEvaluationInput input = model.evaluationInput("credential-a");
    SecurityServiceException missing =
        assertThrows(SecurityServiceException.class, () -> model.evaluationInput("missing"));

    assertEquals(new PrincipalId("principal-a"), input.principalId());
    assertEquals(SecurityServiceDiagnosticCodes.CREDENTIAL_NOT_FOUND, missing.code());
  }

  @Test
  void validatesExternalEvaluationInputAgainstConfiguredLimits() {
    LocalSecurityTrustModel model =
        new LocalSecurityTrustModel(
            new SecurityServiceOptions(2, 2, 8),
            Clock.fixed(Instant.parse("2026-06-20T12:30:00Z"), ZoneOffset.UTC));
    model.registerTrustAnchor("anchor-a", "issuer-a", SecurityCredentialKind.USER);
    SecurityTrustEvaluationInput input =
        new SecurityTrustEvaluationInput(
            new SecurityCredentialId("credential-a"),
            new PrincipalId("principal-a"),
            SecurityCredentialKind.USER,
            new SecurityTrustAnchorId("anchor-a"),
            ISSUED_AT,
            EXPIRES_AT);

    SecurityServiceException malformed =
        assertThrows(SecurityServiceException.class, () -> model.evaluate(input));

    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_IDENTIFIER, malformed.code());
  }

  @Test
  void snapshotsAreImmutableAndIndependent() {
    LocalSecurityTrustModel model = new LocalSecurityTrustModel();
    model.registerTrustAnchor("anchor-a", "issuer-a", SecurityCredentialKind.USER);
    model.registerCredential(
        "credential-a",
        "principal-a",
        SecurityCredentialKind.USER,
        "anchor-a",
        ISSUED_AT,
        EXPIRES_AT);

    SecurityTrustModelSnapshot snapshot = model.snapshot();
    model.registerCredential(
        "credential-b",
        "principal-b",
        SecurityCredentialKind.USER,
        "anchor-a",
        ISSUED_AT,
        EXPIRES_AT);

    assertEquals(1, snapshot.credentials().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> snapshot.credentials().add(snapshot.credentials().get(0)));
    assertThrows(
        UnsupportedOperationException.class,
        () -> model.listCredentials().add(snapshot.credentials().get(0)));
  }
}
