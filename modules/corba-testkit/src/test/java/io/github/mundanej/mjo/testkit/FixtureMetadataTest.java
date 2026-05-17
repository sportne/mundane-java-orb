package io.github.mundanej.mjo.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link FixtureMetadata}. */
@Tag("unit")
final class FixtureMetadataTest {

  @Test
  void createsValidatedFixtureMetadata() {
    FixtureMetadata metadata =
        new FixtureMetadata(
            "IDL_BASIC_PLACEHOLDER",
            FixtureKind.IDL,
            "basic/BasicTypes.idl",
            "Verification fixture");

    assertEquals("IDL_BASIC_PLACEHOLDER", metadata.id());
    assertEquals(FixtureKind.IDL, metadata.kind());
    assertEquals("basic/BasicTypes.idl", metadata.relativePath());
    assertEquals("Verification fixture", metadata.specReference());
  }

  @Test
  void normalizesFixtureMetadataPathSeparators() {
    FixtureMetadata metadata =
        new FixtureMetadata(
            "WIRE_HELLO", FixtureKind.GOLDEN_WIRE, "wire\\hello.bin", "CORBA-IOP-CDR");

    assertEquals("wire/hello.bin", metadata.relativePath());
  }

  @Test
  void rejectsInvalidFixtureMetadata() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new FixtureMetadata(" ", FixtureKind.IDL, "basic/BasicTypes.idl", "IDL-42"));
    assertThrows(
        NullPointerException.class,
        () -> new FixtureMetadata("IDL_BASIC", null, "basic/BasicTypes.idl", "IDL-42"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new FixtureMetadata("IDL_BASIC", FixtureKind.IDL, "../BasicTypes.idl", "IDL-42"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new FixtureMetadata("IDL_BASIC", FixtureKind.IDL, "basic/BasicTypes.idl", " "));
  }

  @Test
  void metadataRecordsHaveValueEquality() {
    FixtureMetadata first =
        new FixtureMetadata("SOURCE_HELLO", FixtureKind.GOLDEN_SOURCE, "hello/Hello.java", "I2JAV");
    FixtureMetadata second =
        new FixtureMetadata("SOURCE_HELLO", FixtureKind.GOLDEN_SOURCE, "hello/Hello.java", "I2JAV");

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }
}
