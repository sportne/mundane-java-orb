package io.github.mundanej.mjo.cdr;

import java.math.BigInteger;
import java.util.Arrays;

/** Native Image smoke entry point for CDR primitive reader and writer behavior. */
public final class CdrNativeSmoke {

  private CdrNativeSmoke() {}

  /** Runs the native smoke assertions. */
  public static void main(String[] args) {
    byte[] longDoublePayload = bytes(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
    BigInteger maxUnsignedLongLong = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);

    CdrWriter writer =
        CdrWriter.littleEndian()
            .writeBoolean(true)
            .writeOctet(0xCA)
            .writeString("native")
            .writeOctetSequence(bytes(1, 1, 2, 3))
            .writeEncapsulation(
                CdrEncapsulation.of(
                    CdrByteOrder.BIG_ENDIAN, bytes(0, 0, 0, 0x00, 0x00, 0x00, 0x2A)))
            .writeLong(-1)
            .writeUnsignedLong(0xFFFFFFFFL)
            .writeUnsignedLongLong(maxUnsignedLongLong)
            .writeFloat(-1.0f)
            .writeDouble(Double.longBitsToDouble(0xC008000000000000L))
            .writeLongDoubleBytes(longDoublePayload);

    CdrReader reader = CdrReader.littleEndian(writer.toByteArray());
    require(reader.readBoolean(), "boolean");
    requireEquals(0xCA, reader.readOctet(), "octet");
    requireEquals("native", reader.readString(), "string");
    require(Arrays.equals(bytes(1, 1, 2, 3), reader.readOctetSequence()), "octet sequence");
    CdrReader nested = reader.readEncapsulation().reader();
    requireEquals(42, nested.readLong(), "encapsulated long");
    requireEquals(0, nested.remaining(), "encapsulated remaining bytes");
    requireEquals(-1, reader.readLong(), "signed long");
    requireEquals(0xFFFFFFFFL, reader.readUnsignedLong(), "unsigned long");
    requireEquals(maxUnsignedLongLong, reader.readUnsignedLongLong(), "unsigned long long");
    requireEquals(0xBF800000, Float.floatToRawIntBits(reader.readFloat()), "float bits");
    requireEquals(
        0xC008000000000000L, Double.doubleToRawLongBits(reader.readDouble()), "double bits");
    require(Arrays.equals(longDoublePayload, reader.readLongDoubleBytes()), "long double bytes");
    requireEquals(0, reader.remaining(), "remaining bytes");
  }

  private static void require(boolean condition, String label) {
    if (!condition) {
      throw new AssertionError("CDR native smoke failed: " + label);
    }
  }

  private static void requireEquals(int expected, int actual, String label) {
    if (expected != actual) {
      throw new AssertionError(
          "CDR native smoke failed: " + label + "; expected " + expected + ", got " + actual);
    }
  }

  private static void requireEquals(long expected, long actual, String label) {
    if (expected != actual) {
      throw new AssertionError(
          "CDR native smoke failed: " + label + "; expected " + expected + ", got " + actual);
    }
  }

  private static void requireEquals(BigInteger expected, BigInteger actual, String label) {
    if (!expected.equals(actual)) {
      throw new AssertionError(
          "CDR native smoke failed: " + label + "; expected " + expected + ", got " + actual);
    }
  }

  private static void requireEquals(String expected, String actual, String label) {
    if (!expected.equals(actual)) {
      throw new AssertionError(
          "CDR native smoke failed: " + label + "; expected " + expected + ", got " + actual);
    }
  }

  private static byte[] bytes(int... values) {
    byte[] bytes = new byte[values.length];
    for (int index = 0; index < values.length; index++) {
      bytes[index] = (byte) values[index];
    }
    return bytes;
  }
}
