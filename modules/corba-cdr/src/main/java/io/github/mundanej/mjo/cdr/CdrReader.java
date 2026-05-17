package io.github.mundanej.mjo.cdr;

import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.common.LimitViolation;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

/** Bounded CDR reader with explicit byte order and alignment handling. */
public final class CdrReader {

  private final CdrByteOrder byteOrder;
  private final CdrLimits limits;
  private final byte[] input;
  private int position;

  /** Creates a primitive reader over a defensive copy of the input bytes. */
  public CdrReader(CdrByteOrder byteOrder, byte[] input) {
    this(byteOrder, input, CdrLimits.defaults());
  }

  /** Creates a reader over a defensive copy of the input bytes with caller-supplied limits. */
  public CdrReader(CdrByteOrder byteOrder, byte[] input, CdrLimits limits) {
    this.byteOrder = Objects.requireNonNull(byteOrder, "byteOrder");
    this.limits = Objects.requireNonNull(limits, "limits");
    this.input = Arrays.copyOf(Objects.requireNonNull(input, "input"), input.length);
  }

  /** Creates a big-endian primitive reader. */
  public static CdrReader bigEndian(byte[] input) {
    return new CdrReader(CdrByteOrder.BIG_ENDIAN, input);
  }

  /** Creates a big-endian reader with caller-supplied limits. */
  public static CdrReader bigEndian(byte[] input, CdrLimits limits) {
    return new CdrReader(CdrByteOrder.BIG_ENDIAN, input, limits);
  }

  /** Creates a little-endian primitive reader. */
  public static CdrReader littleEndian(byte[] input) {
    return new CdrReader(CdrByteOrder.LITTLE_ENDIAN, input);
  }

  /** Creates a little-endian reader with caller-supplied limits. */
  public static CdrReader littleEndian(byte[] input, CdrLimits limits) {
    return new CdrReader(CdrByteOrder.LITTLE_ENDIAN, input, limits);
  }

  /** Returns the configured byte order. */
  public CdrByteOrder byteOrder() {
    return byteOrder;
  }

  /** Returns the configured bounds for length-bearing CDR values. */
  public CdrLimits limits() {
    return limits;
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

  /** Reads a bounded narrow CDR string using one-octet Latin-1 character mapping. */
  public String readString() {
    int length = readRequiredLength("string", limits.stringOctets());
    if (length == 0) {
      throw new CdrException(
          CdrDiagnosticCodes.INVALID_LENGTH, "CDR string length must include a null terminator");
    }
    byte[] bytes = readRawBytes(length, "string octets");
    if (bytes[length - 1] != 0) {
      throw new CdrException(
          CdrDiagnosticCodes.MALFORMED_STRING, "CDR string must end with a null octet");
    }
    char[] characters = new char[length - 1];
    for (int index = 0; index < characters.length; index++) {
      characters[index] = (char) (bytes[index] & 0xFF);
    }
    return new String(characters);
  }

  /** Reads and validates a bounded CDR sequence length. */
  public int readSequenceLength() {
    return readRequiredLength("sequence", limits.sequenceElements());
  }

  /** Validates a fixed-array element count for generated-code loops. */
  public int validateFixedArrayLength(int elementCount) {
    if (elementCount < 0) {
      throw new CdrException(
          CdrDiagnosticCodes.INVALID_COLLECTION_SIZE,
          "CDR fixed-array element count must be nonnegative: " + elementCount);
    }
    limits.sequenceElements().check(elementCount).ifPresent(CdrReader::throwLengthLimitExceeded);
    return elementCount;
  }

  /** Reads a bounded CDR sequence of octets. */
  public byte[] readOctetSequence() {
    return readRawBytes(readSequenceLength(), "octet sequence");
  }

  /** Reads a caller-sized raw octet payload without a CDR length prefix. */
  public byte[] readOctets(int byteCount) {
    if (byteCount < 0) {
      throw new CdrException(
          CdrDiagnosticCodes.INVALID_LENGTH,
          "CDR raw octet count must be nonnegative: " + byteCount);
    }
    return readRawBytes(byteCount, "raw octets");
  }

  /** Reads a bounded length-prefixed CDR encapsulation. */
  public CdrEncapsulation readEncapsulation() {
    int length = readRequiredLength("encapsulation", limits.encapsulationOctets());
    if (length == 0) {
      throw new CdrException(
          CdrDiagnosticCodes.INVALID_LENGTH,
          "CDR encapsulation length must include a byte-order marker");
    }
    return CdrEncapsulation.fromBytes(readRawBytes(length, "encapsulation octets"));
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

  private int readRequiredLength(String label, BoundedLimit limit) {
    long length = readUnsignedLong();
    if (length > Integer.MAX_VALUE) {
      throw new CdrException(
          CdrDiagnosticCodes.INVALID_LENGTH,
          "CDR " + label + " length exceeds Java array limits: " + length);
    }
    limit.check(length).ifPresent(CdrReader::throwLengthLimitExceeded);
    return (int) length;
  }

  private byte[] readRawBytes(int byteCount, String label) {
    requireRemaining(byteCount, label);
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

  private static void throwLengthLimitExceeded(LimitViolation violation) {
    throw new CdrException(CdrDiagnosticCodes.LENGTH_LIMIT_EXCEEDED, violation.message());
  }
}
