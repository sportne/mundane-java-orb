package io.github.mundanej.mjo.cdr;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.testkit.GoldenAssertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit, security, and golden-wire tests for length-bearing CDR values. */
@Tag("unit")
final class CdrCollectionTest {

  @Test
  void writesAndReadsBigEndianStringsAndOctetSequencesWithGoldenWire() {
    CdrWriter writer =
        CdrWriter.bigEndian()
            .writeString("")
            .writeString("H\u00E9")
            .writeOctetSequence(bytes(0xDE, 0xAD));

    byte[] expected =
        bytes(
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x48, 0xE9,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0xDE, 0xAD);

    GoldenAssertions.assertBytesEquals(
        "cdr-big-endian-strings-sequence", expected, writer.toByteArray());

    CdrReader reader = CdrReader.bigEndian(expected);
    assertEquals("", reader.readString());
    assertEquals("H\u00E9", reader.readString());
    assertArrayEquals(bytes(0xDE, 0xAD), reader.readOctetSequence());
    assertEquals(0, reader.remaining());
  }

  @Test
  void writesAndReadsLittleEndianStringsAndSequenceLengthsWithGoldenWire() {
    CdrWriter writer =
        CdrWriter.littleEndian()
            .writeOctet(0xAA)
            .writeString("AZ")
            .writeSequenceLength(2)
            .writeShort((short) 0x1122)
            .writeShort((short) 0x3344);

    byte[] expected =
        bytes(
            0xAA, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x41, 0x5A, 0x00, 0x00, 0x02, 0x00,
            0x00, 0x00, 0x22, 0x11, 0x44, 0x33);

    GoldenAssertions.assertBytesEquals(
        "cdr-little-endian-string-sequence-length", expected, writer.toByteArray());

    CdrReader reader = CdrReader.littleEndian(expected);
    assertEquals(0xAA, reader.readOctet());
    assertEquals("AZ", reader.readString());
    int count = reader.readSequenceLength();
    assertEquals(2, count);
    assertEquals(count, reader.validateFixedArrayLength(count));
    assertEquals((short) 0x1122, reader.readShort());
    assertEquals((short) 0x3344, reader.readShort());
    assertEquals(0, reader.remaining());
  }

  @Test
  void writesAndReadsEncapsulationWithNestedAlignment() {
    CdrEncapsulation encapsulation =
        CdrEncapsulation.of(
            CdrByteOrder.LITTLE_ENDIAN, bytes(0x00, 0x00, 0x00, 0x04, 0x03, 0x02, 0x01));
    CdrWriter writer = CdrWriter.bigEndian().writeOctet(0xCC).writeEncapsulation(encapsulation);
    byte[] expected =
        bytes(
            0xCC, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x01, 0x00, 0x00, 0x00, 0x04, 0x03,
            0x02, 0x01);

    GoldenAssertions.assertBytesEquals(
        "cdr-encapsulation-nested-alignment", expected, writer.toByteArray());

    CdrReader reader = CdrReader.bigEndian(expected);
    assertEquals(0xCC, reader.readOctet());
    CdrEncapsulation decoded = reader.readEncapsulation();
    assertEquals(encapsulation, decoded);
    assertEquals(encapsulation.hashCode(), decoded.hashCode());
    assertEquals("CdrEncapsulation[byteOrder=LITTLE_ENDIAN, byteLength=8]", decoded.toString());

    CdrReader nested = decoded.reader();
    assertEquals(0x01020304, nested.readLong());
    assertEquals(0, nested.remaining());
  }

  @Test
  void defensivelyCopiesOctetSequencesAndEncapsulations() {
    byte[] octets = bytes(0x01, 0x02, 0x03);
    CdrWriter writer = CdrWriter.bigEndian().writeOctetSequence(octets);
    octets[0] = 0x7F;

    byte[] decoded = CdrReader.bigEndian(writer.toByteArray()).readOctetSequence();
    decoded[0] = 0x55;
    assertArrayEquals(
        bytes(0x01, 0x02, 0x03), CdrReader.bigEndian(writer.toByteArray()).readOctetSequence());

    byte[] body = bytes(0x00);
    CdrEncapsulation encapsulation = CdrEncapsulation.of(CdrByteOrder.BIG_ENDIAN, body);
    body[0] = 0x7F;
    byte[] first = encapsulation.bytes();
    first[1] = 0x55;
    assertArrayEquals(bytes(0x00, 0x00), encapsulation.bytes());
    assertEquals(encapsulation, CdrEncapsulation.fromBytes(bytes(0x00, 0x00)));
    assertNotEquals(encapsulation, CdrEncapsulation.fromBytes(bytes(0x00, 0x01)));
    assertNotEquals(encapsulation, "encapsulation");
  }

  @Test
  void rawOctetsRoundTripWithoutLengthPrefixAndUseDefensiveCopies() {
    byte[] octets = bytes(0x01, 0x02, 0x03);
    CdrWriter writer = CdrWriter.bigEndian().writeOctets(octets);
    octets[0] = 0x7F;

    byte[] decoded = CdrReader.bigEndian(writer.toByteArray()).readOctets(3);
    decoded[1] = 0x7E;

    assertArrayEquals(bytes(0x01, 0x02, 0x03), writer.toByteArray());
    assertArrayEquals(
        bytes(0x01, 0x02, 0x03), CdrReader.bigEndian(writer.toByteArray()).readOctets(3));
  }

  @Test
  void rawOctetReadsValidateLengthAndRemainingBytes() {
    assertCdrCode(
        CdrDiagnosticCodes.INVALID_LENGTH, () -> CdrReader.bigEndian(bytes()).readOctets(-1));
    assertCdrCode(
        CdrDiagnosticCodes.TRUNCATED_INPUT, () -> CdrReader.bigEndian(bytes(0x01)).readOctets(2));
  }

  @Test
  void generatedStyleLoopsUseLengthFirstSequenceAndArrayHelpers() {
    CdrWriter writer = CdrWriter.bigEndian();
    int[] values = {3, 5, 8};
    writer.writeSequenceLength(values.length);
    for (int value : values) {
      writer.writeLong(value);
    }
    writer.validateFixedArrayLength(2);
    writer.writeShort((short) 11).writeShort((short) 13);

    CdrReader reader = CdrReader.bigEndian(writer.toByteArray());
    int count = reader.readSequenceLength();
    int[] decoded = new int[count];
    for (int index = 0; index < count; index++) {
      decoded[index] = reader.readLong();
    }
    int fixedCount = reader.validateFixedArrayLength(2);
    short[] fixed = new short[fixedCount];
    for (int index = 0; index < fixedCount; index++) {
      fixed[index] = reader.readShort();
    }

    assertArrayEquals(values, decoded);
    assertArrayEquals(new short[] {11, 13}, fixed);
  }

  @Test
  void reportsMalformedStringsAndInvalidCharacters() {
    assertCdrCode(
        CdrDiagnosticCodes.INVALID_LENGTH,
        () -> CdrReader.bigEndian(bytes(0x00, 0x00, 0x00, 0x00)).readString());
    assertCdrCode(
        CdrDiagnosticCodes.MALFORMED_STRING,
        () -> CdrReader.bigEndian(bytes(0x00, 0x00, 0x00, 0x02, 0x41, 0x42)).readString());
    assertCdrCode(
        CdrDiagnosticCodes.TRUNCATED_INPUT,
        () -> CdrReader.bigEndian(bytes(0x00, 0x00, 0x00, 0x03, 0x41, 0x00)).readString());
    assertCdrCode(
        CdrDiagnosticCodes.INVALID_CHARACTER, () -> CdrWriter.bigEndian().writeString("\u0100"));
  }

  @Test
  void reportsLengthLimitsBeforeAllocatingCollections() {
    CdrLimits limits =
        new CdrLimits(
            new BoundedLimit("test-string", 2),
            new BoundedLimit("test-sequence", 1),
            new BoundedLimit("test-encapsulation", 1));

    assertCdrCode(
        CdrDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
        () -> CdrWriter.bigEndian(limits).writeString("ab"));
    assertCdrCode(
        CdrDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
        () ->
            CdrReader.bigEndian(bytes(0x00, 0x00, 0x00, 0x03, 0x41, 0x42, 0x00), limits)
                .readString());
    assertCdrCode(
        CdrDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
        () -> CdrWriter.bigEndian(limits).writeSequenceLength(2));
    assertCdrCode(
        CdrDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
        () -> CdrReader.bigEndian(bytes(0x00, 0x00, 0x00, 0x02), limits).readSequenceLength());
    assertCdrCode(
        CdrDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
        () ->
            CdrWriter.bigEndian(limits)
                .writeEncapsulation(CdrEncapsulation.of(CdrByteOrder.BIG_ENDIAN, bytes(0x00))));
  }

  @Test
  void reportsInvalidCollectionAndEncapsulationInputs() {
    assertCdrCode(
        CdrDiagnosticCodes.INVALID_COLLECTION_SIZE,
        () -> CdrWriter.bigEndian().writeSequenceLength(-1));
    assertCdrCode(
        CdrDiagnosticCodes.INVALID_COLLECTION_SIZE,
        () -> CdrWriter.bigEndian().validateFixedArrayLength(-1));
    assertCdrCode(
        CdrDiagnosticCodes.INVALID_COLLECTION_SIZE,
        () -> CdrReader.bigEndian(new byte[0]).validateFixedArrayLength(-1));
    assertCdrCode(CdrDiagnosticCodes.INVALID_LENGTH, () -> CdrEncapsulation.fromBytes(new byte[0]));
    assertCdrCode(
        CdrDiagnosticCodes.INVALID_LENGTH,
        () -> CdrReader.bigEndian(bytes(0x00, 0x00, 0x00, 0x00)).readEncapsulation());
    assertCdrCode(
        CdrDiagnosticCodes.INVALID_ENCAPSULATION_BYTE_ORDER,
        () -> CdrEncapsulation.fromBytes(bytes(0x02)));
    assertCdrCode(
        CdrDiagnosticCodes.INVALID_ENCAPSULATION_BYTE_ORDER,
        () -> new CdrEncapsulation(CdrByteOrder.BIG_ENDIAN, bytes(0x01)));
  }

  @Test
  void reportsOutputLimitForLengthBearingPayloadBytes() {
    CdrWriter writer =
        new CdrWriter(
            CdrByteOrder.BIG_ENDIAN, new BoundedLimit("test-output", 4), CdrLimits.defaults());

    assertCdrCode(CdrDiagnosticCodes.OUTPUT_LIMIT_EXCEEDED, () -> writer.writeString("a"));
  }

  @Test
  @Tag("security")
  void hostileLengthFieldsFailBeforePayloadAllocation() {
    CdrLimits strict =
        new CdrLimits(
            new BoundedLimit("test-string", 8),
            new BoundedLimit("test-sequence", 4),
            new BoundedLimit("test-encapsulation", 8));

    int[] hostileLengths = {9, Integer.MAX_VALUE, 0x7FFF_FFFE};
    for (int length : hostileLengths) {
      byte[] lengthPrefix = bigEndianLength(length);
      assertCdrCode(
          CdrDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
          () -> CdrReader.bigEndian(lengthPrefix, strict).readString());
      assertCdrCode(
          CdrDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
          () -> CdrReader.bigEndian(lengthPrefix, strict).readSequenceLength());
      assertCdrCode(
          CdrDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
          () -> CdrReader.bigEndian(lengthPrefix, strict).readEncapsulation());
    }
  }

  @Test
  @Tag("security")
  void boundedCollectionSmokeRemainsDeterministicAcrossRepeatedReads() {
    byte[] payload =
        CdrWriter.bigEndian()
            .writeString("ok")
            .writeSequenceLength(2)
            .writeLong(17)
            .writeLong(19)
            .writeEncapsulation(
                CdrEncapsulation.of(
                    CdrByteOrder.BIG_ENDIAN, bytes(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x17)))
            .toByteArray();

    for (int iteration = 0; iteration < 128; iteration++) {
      CdrReader reader = CdrReader.bigEndian(payload);
      assertEquals("ok", reader.readString());
      assertEquals(2, reader.readSequenceLength());
      assertEquals(17, reader.readLong());
      assertEquals(19, reader.readLong());
      assertEquals(23, reader.readEncapsulation().reader().readLong());
      assertEquals(0, reader.remaining());
    }
  }

  private static void assertCdrCode(Object expectedCode, ThrowingRunnable runnable) {
    CdrException exception = assertThrows(CdrException.class, runnable::run);
    assertEquals(expectedCode, exception.code());
  }

  private static byte[] bigEndianLength(int value) {
    return bytes(value >>> 24, value >>> 16, value >>> 8, value);
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
