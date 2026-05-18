package io.github.mundanej.mjo.ior;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.testkit.GoldenAssertions;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit, negative, and golden-wire tests for IOR binary values. */
@Tag("unit")
final class IorWireTest {

  @Test
  void emitsAndParsesCanonicalNullStringifiedIorWithGoldenWire() {
    String expected = "IOR:00000000000000010000000000000000";

    GoldenAssertions.assertTextEquals(
        "null-stringified-ior", expected, StringifiedIor.format(Ior.nullReference()));

    Ior decoded = StringifiedIor.parse("ior:00000000000000010000000000000000");
    assertEquals("", decoded.typeId());
    assertTrue(decoded.profiles().isEmpty());
  }

  @Test
  void writesAndReadsIorWithIiopProfileGoldenWire() {
    IiopProfile profile =
        new IiopProfile(IiopVersion.V1_0, "h", 9, new ObjectKey(bytes(0x01, 0x02)), List.of());
    Ior ior = new Ior("IDL:Example/Hello:1.0", List.of(TaggedProfile.internetIop(profile)));
    byte[] expected =
        bytes(
            0x00, 0x00, 0x00, 0x16, 0x49, 0x44, 0x4C, 0x3A, 0x45, 0x78, 0x61, 0x6D, 0x70, 0x6C,
            0x65, 0x2F, 0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x3A, 0x31, 0x2E, 0x30, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x12, 0x00, 0x01,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x68, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x02,
            0x01, 0x02);

    GoldenAssertions.assertBytesEquals("ior-iiop-profile-body", expected, ior.toCdrBody());

    Ior decoded = Ior.fromCdrBody(expected);
    IiopProfile decodedProfile = decoded.profiles().get(0).internetIopProfile().orElseThrow();
    assertEquals(ior, decoded);
    assertEquals(IiopVersion.V1_0, decodedProfile.version());
    assertEquals("h", decodedProfile.host());
    assertEquals(9, decodedProfile.port());
    assertArrayEquals(bytes(0x01, 0x02), decodedProfile.objectKey().octets());
  }

  @Test
  void writesAndReadsIiopProfileComponentsAndDefensiveCopies() {
    byte[] componentBytes = bytes(0xDE, 0xAD);
    byte[] keyBytes = bytes(0x41, 0x42, 0x43);
    TaggedComponent component = new TaggedComponent(0x8000_0001L, componentBytes);
    IiopProfile profile =
        new IiopProfile(
            IiopVersion.V1_2, "orb.example", 2809, new ObjectKey(keyBytes), List.of(component));
    componentBytes[0] = 0x00;
    keyBytes[0] = 0x00;

    IiopProfile decoded = IiopProfile.fromProfileData(profile.toProfileData());
    byte[] firstKey = decoded.objectKey().octets();
    firstKey[0] = 0x00;

    assertEquals(profile, decoded);
    assertEquals(profile.hashCode(), decoded.hashCode());
    assertEquals(IiopVersion.V1_2, decoded.version());
    assertEquals(
        "IiopProfile[version=1.2, host=orb.example, port=2809, objectKeyOctets=3, components=1]",
        decoded.toString());
    assertArrayEquals(bytes(0x41, 0x42, 0x43), decoded.objectKey().octets());
    assertArrayEquals(bytes(0xDE, 0xAD), decoded.components().get(0).componentData());
  }

  @Test
  void preservesUnknownTaggedProfilesAndComponents() {
    TaggedProfile profile = new TaggedProfile(0x8000_0000L, bytes(0x01, 0x02, 0x03));
    TaggedComponent component = new TaggedComponent(7, bytes(0x04));

    assertEquals(profile, new TaggedProfile(0x8000_0000L, bytes(0x01, 0x02, 0x03)));
    assertEquals(
        profile.hashCode(), new TaggedProfile(0x8000_0000L, bytes(0x01, 0x02, 0x03)).hashCode());
    assertNotEquals(profile, new TaggedProfile(0x8000_0001L, bytes(0x01, 0x02, 0x03)));
    assertEquals("TaggedProfile[tag=2147483648, dataLength=3]", profile.toString());
    assertEquals(component, new TaggedComponent(7, bytes(0x04)));
    assertNotEquals(component, new TaggedComponent(8, bytes(0x04)));
    assertEquals("TaggedComponent[tag=7, dataLength=1]", component.toString());
  }

  @Test
  void reportsInvalidStringifiedIorInputs() {
    IorLimits strict =
        new IorLimits(
            new BoundedLimit("test-string", 32),
            new BoundedLimit("test-sequence", 32),
            new BoundedLimit("test-encapsulation", 1),
            new BoundedLimit("test-profile-count", 1),
            new BoundedLimit("test-profile-data", 32),
            new BoundedLimit("test-component-count", 1),
            new BoundedLimit("test-component-data", 32),
            new BoundedLimit("test-object-key", 1),
            new BoundedLimit("test-url", 32));

    assertIorCode(IorDiagnosticCodes.INVALID_STRINGIFIED_IOR, () -> StringifiedIor.parse("http:x"));
    assertIorCode(IorDiagnosticCodes.INVALID_STRINGIFIED_IOR, () -> StringifiedIor.parse("IOR:"));
    assertIorCode(IorDiagnosticCodes.INVALID_STRINGIFIED_IOR, () -> StringifiedIor.parse("IOR:0"));
    assertIorCode(IorDiagnosticCodes.INVALID_STRINGIFIED_IOR, () -> StringifiedIor.parse("IOR:zz"));
    assertIorCode(
        IorDiagnosticCodes.LENGTH_LIMIT_EXCEEDED, () -> StringifiedIor.parse("IOR:0000", strict));
  }

  @Test
  void reportsInvalidBinaryInputsAndLimits() {
    IorLimits strict =
        new IorLimits(
            new BoundedLimit("test-string", 32),
            new BoundedLimit("test-sequence", 32),
            new BoundedLimit("test-encapsulation", 32),
            new BoundedLimit("test-profile-count", 0),
            new BoundedLimit("test-profile-data", 1),
            new BoundedLimit("test-component-count", 0),
            new BoundedLimit("test-component-data", 0),
            new BoundedLimit("test-object-key", 1),
            new BoundedLimit("test-url", 32));

    assertIorCode(IorDiagnosticCodes.TAG_OUT_OF_RANGE, () -> new TaggedProfile(-1, bytes()));
    assertIorCode(
        IorDiagnosticCodes.TAG_OUT_OF_RANGE, () -> new TaggedComponent(0x1_0000_0000L, bytes()));
    assertIorCode(
        IorDiagnosticCodes.INVALID_PORT,
        () -> new IiopProfile(IiopVersion.V1_0, "h", 0x1_0000, ObjectKey.empty(), List.of()));
    assertIorCode(
        IorDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
        () -> new TaggedProfile(0, bytes(0x01, 0x02), strict));
    assertIorCode(
        IorDiagnosticCodes.INVALID_IIOP_PROFILE,
        () -> new IiopProfile(new IiopVersion(2, 0), "h", 1, ObjectKey.empty(), List.of()));
    assertIorCode(
        IorDiagnosticCodes.INVALID_IIOP_PROFILE,
        () -> new IiopProfile(IiopVersion.V1_0, "", 1, ObjectKey.empty(), List.of()));
    assertIorCode(
        IorDiagnosticCodes.INVALID_IIOP_PROFILE,
        () ->
            new IiopProfile(
                IiopVersion.V1_0,
                "h",
                1,
                ObjectKey.empty(),
                List.of(new TaggedComponent(1, bytes()))));
    assertIorCode(
        IorDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
        () ->
            Ior.fromCdrBody(
                new Ior("IDL:X:1.0", List.of(new TaggedProfile(1, bytes()))).toCdrBody(), strict));
  }

  @Test
  @Tag("security")
  void hostileStringifiedIorAndProfileCountsFailBeforeAllocation() {
    IorLimits strict =
        new IorLimits(
            new BoundedLimit("test-string", 32),
            new BoundedLimit("test-sequence", 32),
            new BoundedLimit("test-encapsulation", 8),
            new BoundedLimit("test-profile-count", 1),
            new BoundedLimit("test-profile-data", 8),
            new BoundedLimit("test-component-count", 1),
            new BoundedLimit("test-component-data", 8),
            new BoundedLimit("test-object-key", 8),
            new BoundedLimit("test-url", 32));

    assertIorCode(
        IorDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
        () -> StringifiedIor.parse("IOR:000000000000000000", strict));
    assertIorCode(
        IorDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
        () ->
            Ior.fromCdrBody(
                bytes(0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02),
                strict));
  }

  @Test
  @Tag("security")
  void boundedIorSmokeRemainsDeterministicAcrossRepeatedReads() {
    Ior ior =
        new Ior(
            "IDL:Example/Bounded:1.0",
            List.of(
                TaggedProfile.internetIop(
                    new IiopProfile(
                        IiopVersion.V1_2,
                        "localhost",
                        2809,
                        new ObjectKey(bytes(0x01, 0x02)),
                        List.of(new TaggedComponent(5, bytes(0xCA)))))));
    String stringified = StringifiedIor.format(ior);

    for (int iteration = 0; iteration < 128; iteration++) {
      Ior decoded = StringifiedIor.parse(stringified);
      assertEquals(ior, decoded);
      assertArrayEquals(
          bytes(0x01, 0x02),
          decoded.profiles().getFirst().internetIopProfile().orElseThrow().objectKey().octets());
    }
  }

  private static void assertIorCode(Object expectedCode, ThrowingRunnable runnable) {
    IorException exception = assertThrows(IorException.class, runnable::run);
    assertEquals(expectedCode, exception.code());
  }

  private static byte[] bytes(int... values) {
    byte[] bytes = new byte[values.length];
    for (int index = 0; index < values.length; index++) {
      bytes[index] = (byte) values[index];
    }
    return bytes;
  }

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run();
  }
}
