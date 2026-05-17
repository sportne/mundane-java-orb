package io.github.mundanej.mjo.cdr;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.testkit.GoldenAssertions;
import java.math.BigInteger;
import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit and golden-wire tests for CDR primitive encoding. */
@Tag("unit")
final class CdrPrimitiveTest {

  @Test
  void writesAndReadsBigEndianPrimitiveGoldenWire() {
    byte[] longDoublePayload = longDoublePayload();
    CdrWriter writer =
        CdrWriter.bigEndian()
            .writeBoolean(true)
            .writeOctet(0x7F)
            .writeChar('A')
            .writeShort((short) 0x1234)
            .writeUnsignedShort(0xFEDC)
            .writeLong(0x01020304)
            .writeUnsignedLong(0x89ABCDEFL)
            .writeLongLong(0x0102030405060708L)
            .writeFloat(Float.intBitsToFloat(0x3F800000))
            .writeDouble(Double.longBitsToDouble(0x4008000000000000L))
            .writeLongDoubleBytes(longDoublePayload);

    byte[] expected =
        bytes(
            0x01, 0x7F, 0x41, 0x00, 0x12, 0x34, 0xFE, 0xDC, 0x01, 0x02, 0x03, 0x04, 0x89, 0xAB,
            0xCD, 0xEF, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x3F, 0x80, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x40, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
            0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F);

    GoldenAssertions.assertBytesEquals("cdr-big-endian-primitives", expected, writer.toByteArray());

    CdrReader reader = CdrReader.bigEndian(expected);
    assertTrue(reader.readBoolean());
    assertEquals(0x7F, reader.readOctet());
    assertEquals('A', reader.readChar());
    assertEquals((short) 0x1234, reader.readShort());
    assertEquals(0xFEDC, reader.readUnsignedShort());
    assertEquals(0x01020304, reader.readLong());
    assertEquals(0x89ABCDEFL, reader.readUnsignedLong());
    assertEquals(0x0102030405060708L, reader.readLongLong());
    assertEquals(0x3F800000, Float.floatToRawIntBits(reader.readFloat()));
    assertEquals(0x4008000000000000L, Double.doubleToRawLongBits(reader.readDouble()));
    assertArrayEquals(longDoublePayload, reader.readLongDoubleBytes());
    assertEquals(0, reader.remaining());
  }

  @Test
  void writesAndReadsLittleEndianPrimitiveGoldenWireWithAlignmentPadding() {
    CdrWriter writer =
        CdrWriter.littleEndian()
            .writeOctet(0xAA)
            .writeLong(0x01020304)
            .writeShort((short) 0x1122)
            .writeLongLong(0x0102030405060708L);

    byte[] expected =
        bytes(
            0xAA, 0x00, 0x00, 0x00, 0x04, 0x03, 0x02, 0x01, 0x22, 0x11, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01);

    GoldenAssertions.assertBytesEquals(
        "cdr-little-endian-alignment", expected, writer.toByteArray());

    CdrReader reader = CdrReader.littleEndian(expected);
    assertEquals(CdrByteOrder.LITTLE_ENDIAN, reader.byteOrder());
    assertEquals(0xAA, reader.readOctet());
    assertEquals(0x01020304, reader.readLong());
    assertEquals((short) 0x1122, reader.readShort());
    assertEquals(0x0102030405060708L, reader.readLongLong());
    assertEquals(expected.length, reader.position());
  }

  @Test
  void handlesUnsignedBoundaryValues() {
    BigInteger maxUnsignedLongLong = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
    CdrWriter writer =
        CdrWriter.bigEndian()
            .writeUnsignedShort(0xFFFF)
            .writeUnsignedLong(0xFFFFFFFFL)
            .writeUnsignedLongLong(maxUnsignedLongLong);

    CdrReader reader = CdrReader.bigEndian(writer.toByteArray());

    assertEquals(0xFFFF, reader.readUnsignedShort());
    assertEquals(0xFFFFFFFFL, reader.readUnsignedLong());
    assertEquals(maxUnsignedLongLong, reader.readUnsignedLongLong());
  }

  @Test
  void preservesSignedLongAndFloatHighBitPatterns() {
    CdrWriter bigEndian =
        CdrWriter.bigEndian()
            .writeLong(-1)
            .writeLong(Integer.MIN_VALUE)
            .writeFloat(-1.0f)
            .writeFloat(Float.intBitsToFloat(0xFFC00000));
    CdrWriter littleEndian =
        CdrWriter.littleEndian()
            .writeLong(-1)
            .writeLong(Integer.MIN_VALUE)
            .writeFloat(-1.0f)
            .writeFloat(Float.intBitsToFloat(0xFFC00000));

    assertSignedHighBitValues(CdrReader.bigEndian(bigEndian.toByteArray()));
    assertSignedHighBitValues(CdrReader.littleEndian(littleEndian.toByteArray()));
  }

  @Test
  void defensivelyCopiesInputAndOutputArrays() {
    byte[] input = bytes(0x00, 0x00, 0x00, 0x01);
    CdrReader reader = CdrReader.bigEndian(input);
    input[3] = 0x7F;

    assertEquals(1, reader.readLong());

    byte[] longDoublePayload = longDoublePayload();
    CdrWriter writer = CdrWriter.bigEndian().writeLongDoubleBytes(longDoublePayload);
    longDoublePayload[0] = 0x55;
    byte[] firstOutput = writer.toByteArray();
    firstOutput[0] = 0x66;

    assertEquals(0x00, writer.toByteArray()[0]);
  }

  @Test
  void reportsTruncatedInputAndStrictBooleanFailures() {
    CdrException truncated =
        assertThrows(CdrException.class, () -> CdrReader.bigEndian(bytes(0x00, 0x00)).readLong());
    CdrException invalidBoolean =
        assertThrows(CdrException.class, () -> CdrReader.bigEndian(bytes(0x02)).readBoolean());

    assertEquals(CdrDiagnosticCodes.TRUNCATED_INPUT, truncated.code());
    assertEquals(CdrDiagnosticCodes.INVALID_BOOLEAN, invalidBoolean.code());
  }

  @Test
  void reportsInvalidWriterInputsAndOutputLimitFailures() {
    CdrWriter writer = CdrWriter.bigEndian();

    assertCdrCode(CdrDiagnosticCodes.INVALID_CHARACTER, () -> writer.writeChar('\u0100'));
    assertCdrCode(CdrDiagnosticCodes.UNSIGNED_VALUE_OUT_OF_RANGE, () -> writer.writeOctet(256));
    assertCdrCode(
        CdrDiagnosticCodes.UNSIGNED_VALUE_OUT_OF_RANGE, () -> writer.writeUnsignedShort(-1));
    assertCdrCode(
        CdrDiagnosticCodes.UNSIGNED_VALUE_OUT_OF_RANGE,
        () -> writer.writeUnsignedLong(0x1_0000_0000L));
    assertCdrCode(
        CdrDiagnosticCodes.UNSIGNED_VALUE_OUT_OF_RANGE,
        () -> writer.writeUnsignedLongLong(BigInteger.ONE.shiftLeft(64)));
    assertCdrCode(
        CdrDiagnosticCodes.INVALID_LONG_DOUBLE, () -> writer.writeLongDoubleBytes(new byte[15]));
    assertCdrCode(
        CdrDiagnosticCodes.OUTPUT_LIMIT_EXCEEDED,
        () ->
            new CdrWriter(CdrByteOrder.BIG_ENDIAN, new BoundedLimit("test-output", 3))
                .writeLong(1));
  }

  @Test
  void validatesPublicAlignmentArguments() {
    assertThrows(IllegalArgumentException.class, () -> CdrWriter.bigEndian().align(3));
    assertThrows(IllegalArgumentException.class, () -> CdrReader.bigEndian(new byte[0]).align(0));

    CdrWriter writer = CdrWriter.bigEndian().writeOctet(1).align(8);
    assertEquals(8, writer.position());
    assertTrue(
        Arrays.stream(toUnsigned(writer.toByteArray())).skip(1).allMatch(value -> value == 0));
  }

  private static void assertCdrCode(Object expectedCode, ThrowingRunnable runnable) {
    CdrException exception = assertThrows(CdrException.class, runnable::run);
    assertEquals(expectedCode, exception.code());
  }

  private static void assertSignedHighBitValues(CdrReader reader) {
    assertEquals(-1, reader.readLong());
    assertEquals(Integer.MIN_VALUE, reader.readLong());
    assertEquals(0xBF800000, Float.floatToRawIntBits(reader.readFloat()));
    assertEquals(0xFFC00000, Float.floatToRawIntBits(reader.readFloat()));
  }

  private static byte[] longDoublePayload() {
    return bytes(
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
        0x0F);
  }

  private static byte[] bytes(int... values) {
    byte[] bytes = new byte[values.length];
    for (int index = 0; index < values.length; index++) {
      bytes[index] = (byte) values[index];
    }
    return bytes;
  }

  private static int[] toUnsigned(byte[] bytes) {
    int[] values = new int[bytes.length];
    for (int index = 0; index < bytes.length; index++) {
      values[index] = bytes[index] & 0xFF;
    }
    return values;
  }

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run();
  }
}
