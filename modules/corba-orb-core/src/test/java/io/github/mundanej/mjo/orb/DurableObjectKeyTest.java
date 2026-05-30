package io.github.mundanej.mjo.orb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
final class DurableObjectKeyTest {

  @Test
  void durableObjectKeysRoundTripThroughBoundedVersionedBytes() {
    DurableObjectKey key =
        new DurableObjectKey("orb-1", List.of("RootPOA", "apps"), new byte[] {1, 2, 3}, 7);

    DurableObjectKey decoded = DurableObjectKey.decode(key.encode());

    assertEquals(key, decoded);
    assertEquals("orb-1", decoded.orbId());
    assertEquals(List.of("RootPOA", "apps"), decoded.poaPath());
    assertEquals("/RootPOA/apps", decoded.poaPathString());
    assertArrayEquals(new byte[] {1, 2, 3}, decoded.objectId());
    assertEquals(7, decoded.flags());
    assertTrue(DurableObjectKey.hasDurablePrefix(key.encode()));
  }

  @Test
  void durableObjectKeysRejectMalformedValuesBeforeAllocation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new DurableObjectKey(" ", List.of("RootPOA"), new byte[] {1}, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new DurableObjectKey("orb-1", List.of(), new byte[] {1}, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new DurableObjectKey("orb-1", List.of("Root/POA"), new byte[] {1}, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new DurableObjectKey("orb-1", List.of(".."), new byte[] {1}, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> DurableObjectKey.fromPoaPath("orb-1", "/RootPOA/../admin", new byte[] {1}, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new DurableObjectKey("orb-1", List.of("RootPOA"), new byte[0], 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new DurableObjectKey("orb-1", List.of("RootPOA"), new byte[] {1}, 256));
  }

  @Test
  void durableObjectKeyDecodeRejectsTransientAndHostileInputs() {
    DurableObjectKey key =
        DurableObjectKey.fromPoaPath("orb-1", "/RootPOA/apps", new byte[] {1}, 0);
    byte[] encoded = key.encode();
    byte[] badVersion = encoded.clone();
    badVersion[4] = 99;

    assertFalse(DurableObjectKey.hasDurablePrefix("transient".getBytes(StandardCharsets.US_ASCII)));
    assertThrows(IllegalArgumentException.class, () -> DurableObjectKey.decode(new byte[] {1, 2}));
    assertThrows(IllegalArgumentException.class, () -> DurableObjectKey.decode(badVersion));
    assertThrows(
        IllegalArgumentException.class,
        () -> DurableObjectKey.decode(encodedKeyWithTraversalComponent()));
    assertThrows(IllegalArgumentException.class, () -> DurableObjectKey.decode(new byte[65_537]));
  }

  private static byte[] encodedKeyWithTraversalComponent() {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.writeBytes(new byte[] {'M', 'J', 'O', 'K', 1, 0});
    writeAscii(output, "orb-1");
    writeUnsignedShort(output, 1);
    writeAscii(output, "..");
    writeUnsignedShort(output, 1);
    output.write(1);
    return output.toByteArray();
  }

  private static void writeAscii(ByteArrayOutputStream output, String value) {
    byte[] octets = value.getBytes(StandardCharsets.US_ASCII);
    writeUnsignedShort(output, octets.length);
    output.writeBytes(octets);
  }

  private static void writeUnsignedShort(ByteArrayOutputStream output, int value) {
    output.write((value >>> 8) & 0xFF);
    output.write(value & 0xFF);
  }
}
