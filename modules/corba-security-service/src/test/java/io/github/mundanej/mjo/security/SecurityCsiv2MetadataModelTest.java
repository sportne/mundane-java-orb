package io.github.mundanej.mjo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SecurityCsiv2MetadataModelTest {

  @Test
  void createsDefaultMetadata() {
    SecurityCsiv2MetadataSnapshot snapshot = new SecurityCsiv2MetadataModel().defaults();

    assertEquals(1, snapshot.mechanisms().size());
    SecurityCsiv2Mechanism mechanism = snapshot.mechanisms().getFirst();
    assertEquals(SecurityCsiv2MechanismIds.SUPPORTED_LOCAL, mechanism.mechanismId());
    assertEquals(SecurityTransportProtection.NONE, mechanism.transportProtection());
    assertEquals(SecurityCsiv2IdentityTokenPolicy.ABSENT, mechanism.identityTokenPolicy());
    assertEquals(SecurityAuthenticationRequirement.OPTIONAL, mechanism.targetAuthentication());
    assertEquals(SecurityAuthenticationRequirement.OPTIONAL, mechanism.clientAuthentication());
  }

  @Test
  void roundTripsExplicitMetadata() {
    SecurityCsiv2MetadataCodec codec = new SecurityCsiv2MetadataCodec();
    SecurityCsiv2MetadataSnapshot snapshot =
        new SecurityCsiv2MetadataModel()
            .validate(
                List.of(
                    new SecurityCsiv2Mechanism(
                        SecurityCsiv2MechanismIds.SUPPORTED_LOCAL,
                        SecurityTransportProtection.CONFIDENTIALITY,
                        SecurityCsiv2IdentityTokenPolicy.PRINCIPAL_NAME,
                        SecurityAuthenticationRequirement.REQUIRED,
                        SecurityAuthenticationRequirement.REQUIRED)));

    SecurityCsiv2MetadataSnapshot decoded = codec.decode(codec.encode(snapshot));

    assertEquals(snapshot, decoded);
  }

  @Test
  void rejectsMalformedMetadataText() {
    SecurityCsiv2MetadataCodec codec = new SecurityCsiv2MetadataCodec();
    SecurityServiceException blank =
        assertThrows(SecurityServiceException.class, () -> codec.decode(" "));
    SecurityServiceException wrongVersion =
        assertThrows(SecurityServiceException.class, () -> codec.decode("other|1|x"));
    SecurityServiceException badCount =
        assertThrows(
            SecurityServiceException.class,
            () -> codec.decode(SecurityCsiv2MetadataCodec.VERSION + "|many|x"));
    SecurityServiceException mismatchedCount =
        assertThrows(
            SecurityServiceException.class,
            () -> codec.decode(SecurityCsiv2MetadataCodec.VERSION + "|2|x"));
    SecurityServiceException badMechanismField =
        assertThrows(
            SecurityServiceException.class,
            () -> codec.decode(SecurityCsiv2MetadataCodec.VERSION + "|1|not-base64"));

    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA, blank.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA, wrongVersion.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA, badCount.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA, mismatchedCount.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA, badMechanismField.code());
  }

  @Test
  void rejectsOversizedInputsAndConfiguredCounts() {
    SecurityCsiv2MetadataModel model =
        new SecurityCsiv2MetadataModel(new SecurityCsiv2MetadataOptions(1, 256));
    SecurityCsiv2MetadataCodec codec =
        new SecurityCsiv2MetadataCodec(new SecurityCsiv2MetadataOptions(2, 128));
    SecurityCsiv2Mechanism mechanism = SecurityCsiv2Mechanism.defaults();
    SecurityServiceException invalidOptions =
        assertThrows(
            SecurityServiceException.class, () -> new SecurityCsiv2MetadataOptions(0, 128));
    SecurityServiceException countLimit =
        assertThrows(
            SecurityServiceException.class, () -> model.validate(List.of(mechanism, mechanism)));
    SecurityServiceException encodedLimit =
        assertThrows(
            SecurityServiceException.class,
            () -> codec.decode(SecurityCsiv2MetadataCodec.VERSION + "|1|" + "x".repeat(200)));
    SecurityServiceException mechanismIdLimit =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityCsiv2Mechanism(
                    "x".repeat(SecurityCsiv2MechanismIds.MAX_MECHANISM_ID_LENGTH + 1),
                    SecurityTransportProtection.NONE,
                    SecurityCsiv2IdentityTokenPolicy.ABSENT,
                    SecurityAuthenticationRequirement.OPTIONAL,
                    SecurityAuthenticationRequirement.OPTIONAL));
    SecurityServiceException snapshotCountLimit =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityCsiv2MetadataSnapshot(
                    Collections.nCopies(
                        SecurityCsiv2MetadataOptions.ABSOLUTE_MAX_MECHANISMS + 1, mechanism)));

    assertEquals(SecurityServiceDiagnosticCodes.INVALID_LIMIT, invalidOptions.code());
    assertEquals(SecurityServiceDiagnosticCodes.CSIV2_METADATA_LIMIT_EXCEEDED, countLimit.code());
    assertEquals(SecurityServiceDiagnosticCodes.CSIV2_METADATA_LIMIT_EXCEEDED, encodedLimit.code());
    assertEquals(
        SecurityServiceDiagnosticCodes.CSIV2_METADATA_LIMIT_EXCEEDED, mechanismIdLimit.code());
    assertEquals(
        SecurityServiceDiagnosticCodes.CSIV2_METADATA_LIMIT_EXCEEDED, snapshotCountLimit.code());
  }

  @Test
  void rejectsUnsupportedMechanisms() {
    SecurityServiceException unsupported =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityCsiv2Mechanism(
                    "UNSUPPORTED",
                    SecurityTransportProtection.NONE,
                    SecurityCsiv2IdentityTokenPolicy.ABSENT,
                    SecurityAuthenticationRequirement.OPTIONAL,
                    SecurityAuthenticationRequirement.OPTIONAL));
    String encodedUnsupported =
        SecurityCsiv2MetadataCodec.VERSION + "|1|VU5TVVBQT1JURUQ:NONE:ABSENT:OPTIONAL:OPTIONAL";
    SecurityServiceException decodedUnsupported =
        assertThrows(
            SecurityServiceException.class,
            () -> new SecurityCsiv2MetadataCodec().decode(encodedUnsupported));

    assertEquals(SecurityServiceDiagnosticCodes.UNSUPPORTED_CSIV2_MECHANISM, unsupported.code());
    assertEquals(
        SecurityServiceDiagnosticCodes.UNSUPPORTED_CSIV2_MECHANISM, decodedUnsupported.code());
  }

  @Test
  void rejectsIncompleteAuthenticationMetadata() {
    SecurityServiceException missingTransport =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityCsiv2Mechanism(
                    SecurityCsiv2MechanismIds.SUPPORTED_LOCAL,
                    null,
                    SecurityCsiv2IdentityTokenPolicy.ABSENT,
                    SecurityAuthenticationRequirement.OPTIONAL,
                    SecurityAuthenticationRequirement.OPTIONAL));
    SecurityServiceException missingTarget =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityCsiv2Mechanism(
                    SecurityCsiv2MechanismIds.SUPPORTED_LOCAL,
                    SecurityTransportProtection.NONE,
                    SecurityCsiv2IdentityTokenPolicy.ABSENT,
                    null,
                    SecurityAuthenticationRequirement.OPTIONAL));
    SecurityServiceException missingClient =
        assertThrows(
            SecurityServiceException.class,
            () ->
                new SecurityCsiv2Mechanism(
                    SecurityCsiv2MechanismIds.SUPPORTED_LOCAL,
                    SecurityTransportProtection.NONE,
                    SecurityCsiv2IdentityTokenPolicy.ABSENT,
                    SecurityAuthenticationRequirement.OPTIONAL,
                    null));
    SecurityServiceException nullList =
        assertThrows(
            SecurityServiceException.class, () -> new SecurityCsiv2MetadataModel().validate(null));
    SecurityServiceException nullMechanism =
        assertThrows(
            SecurityServiceException.class,
            () -> new SecurityCsiv2MetadataModel().validate(Collections.singletonList(null)));

    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA, missingTransport.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA, missingTarget.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA, missingClient.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA, nullList.code());
    assertEquals(SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA, nullMechanism.code());
  }

  @Test
  void snapshotsAreImmutable() {
    SecurityCsiv2MetadataSnapshot snapshot = new SecurityCsiv2MetadataModel().defaults();

    assertThrows(
        UnsupportedOperationException.class,
        () -> snapshot.mechanisms().add(SecurityCsiv2Mechanism.defaults()));
  }
}
