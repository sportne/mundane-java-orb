package io.github.mundanej.mjo.cdr;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

/** Bounded CDR primitive reader with explicit byte order and alignment handling. */
public final class CdrReader {

  private final CdrByteOrder byteOrder;
  private final byte[] input;
  private int position;

  /** Creates a primitive reader over a defensive copy of the input bytes. */
  public CdrReader(CdrByteOrder byteOrder, byte[] input) {
    this.byteOrder = Objects.requireNonNull(byteOrder, "byteOrder");
    this.input = Arrays.copyOf(Objects.requireNonNull(input, "input"), input.length);
  }

  /** Creates a big-endian primitive reader. */
  public static CdrReader bigEndian(byte[] input) {
    return new CdrReader(CdrByteOrder.BIG_ENDIAN, input);
  }

  /** Creates a little-endian primitive reader. */
  public static CdrReader littleEndian(byte[] input) {
    return new CdrReader(CdrByteOrder.LITTLE_ENDIAN, input);
  }

  /** Returns the configured byte order. */
  public CdrByteOrder byteOrder() {
    return byteOrder;
  }

  /** Returns the next byte offset to be read. */
  public int position() {
    return position;
  }

  /** Returns the unread byte count. */
  public int remaining() {
    return input.length - position;
  }

  /** Advances to the next offset aligned for the supplied byte boundary. */
  public CdrReader align(int alignment) {
    int padding = paddingFor(position, alignment);
    requireRemaining(padding, "alignment padding");
    position += padding;
    return this;
  }

  /** Reads a strict CDR boolean value. */
  public boolean readBoolean() {
    int value = readUnsigned(1, 1);
    if (value == 0) {
      return false;
    }
    if (value == 1) {
      return true;
    }
    throw new CdrException(
        CdrDiagnosticCodes.INVALID_BOOLEAN, "CDR boolean octet must be 0 or 1: " + value);
  }

  /** Reads an unsigned CDR octet value. */
  public int readOctet() {
    return readUnsigned(1, 1);
  }

  /** Reads a CDR char value from one octet. */
  public char readChar() {
    return (char) readUnsigned(1, 1);
  }

  /** Reads a signed CDR short value. */
  public short readShort() {
    return (short) readUnsigned(2, 2);
  }

  /** Reads an unsigned CDR short value. */
  public int readUnsignedShort() {
    return readUnsigned(2, 2);
  }

  /** Reads a signed CDR long value. */
  public int readLong() {
    return (int) readUnsignedLongBits(4, 4);
  }

  /** Reads an unsigned CDR long value. */
  public long readUnsignedLong() {
    return readUnsignedLongBits(4, 4);
  }

  /** Reads a signed CDR long long value. */
  public long readLongLong() {
    return readUnsignedLongBits(8, 8);
  }

  /** Reads an unsigned CDR long long value. */
  public BigInteger readUnsignedLongLong() {
    byte[] bytes = readPrimitiveBytes(8, 8);
    if (byteOrder == CdrByteOrder.LITTLE_ENDIAN) {
      reverse(bytes);
    }
    return new BigInteger(1, bytes);
  }

  /** Reads a CDR float value. */
  public float readFloat() {
    return Float.intBitsToFloat(readLong());
  }

  /** Reads a CDR double value. */
  public double readDouble() {
    return Double.longBitsToDouble(readLongLong());
  }

  /** Reads the raw 16-octet CDR long double payload. */
  public byte[] readLongDoubleBytes() {
    return readPrimitiveBytes(8, 16);
  }

  private int readUnsigned(int alignment, int byteCount) {
    long value = readUnsignedLongBits(alignment, byteCount);
    return Math.toIntExact(value);
  }

  private long readUnsignedLongBits(int alignment, int byteCount) {
    byte[] bytes = readPrimitiveBytes(alignment, byteCount);
    long value = 0L;
    if (byteOrder == CdrByteOrder.BIG_ENDIAN) {
      for (byte current : bytes) {
        value = (value << 8) | (current & 0xFFL);
      }
    } else {
      for (int index = bytes.length - 1; index >= 0; index--) {
        value = (value << 8) | (bytes[index] & 0xFFL);
      }
    }
    return value;
  }

  private byte[] readPrimitiveBytes(int alignment, int byteCount) {
    int padding = paddingFor(position, alignment);
    requireRemaining(padding + byteCount, "aligned primitive");
    position += padding;
    byte[] value = Arrays.copyOfRange(input, position, position + byteCount);
    position += byteCount;
    return value;
  }

  private void requireRemaining(int byteCount, String label) {
    if (byteCount > remaining()) {
      throw new CdrException(
          CdrDiagnosticCodes.TRUNCATED_INPUT,
          "CDR input ended before "
              + label
              + "; need "
              + byteCount
              + " byte(s), remaining "
              + remaining());
    }
  }

  private static int paddingFor(int position, int alignment) {
    if (alignment <= 0 || Integer.bitCount(alignment) != 1 || alignment > 8) {
      throw new IllegalArgumentException("alignment must be a power of two in 1..8");
    }
    int remainder = position % alignment;
    return remainder == 0 ? 0 : alignment - remainder;
  }

  private static void reverse(byte[] bytes) {
    for (int left = 0, right = bytes.length - 1; left < right; left++, right--) {
      byte temporary = bytes[left];
      bytes[left] = bytes[right];
      bytes[right] = temporary;
    }
  }
}
