package io.github.mundanej.mjo.cdr;

import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.common.LimitViolation;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

/** Bounded CDR writer with explicit byte order and alignment handling. */
public final class CdrWriter {

  private static final BoundedLimit DEFAULT_OUTPUT_LENGTH_LIMIT =
      new BoundedLimit("cdr-output-length", 1_048_576L);
  private static final BigInteger UNSIGNED_LONG_LONG_LIMIT = BigInteger.ONE.shiftLeft(64);

  private final CdrByteOrder byteOrder;
  private final CdrLimits limits;
  private final BoundedLimit outputLengthLimit;
  private byte[] output = new byte[32];
  private int position;

  /** Creates a primitive writer with the default output bound. */
  public CdrWriter(CdrByteOrder byteOrder) {
    this(byteOrder, DEFAULT_OUTPUT_LENGTH_LIMIT, CdrLimits.defaults());
  }

  /** Creates a writer with caller-supplied CDR limits and the default output bound. */
  public CdrWriter(CdrByteOrder byteOrder, CdrLimits limits) {
    this(byteOrder, DEFAULT_OUTPUT_LENGTH_LIMIT, limits);
  }

  /** Creates a primitive writer with a caller-supplied output bound. */
  public CdrWriter(CdrByteOrder byteOrder, BoundedLimit outputLengthLimit) {
    this(byteOrder, outputLengthLimit, CdrLimits.defaults());
  }

  /** Creates a writer with caller-supplied output and length-bearing value bounds. */
  public CdrWriter(CdrByteOrder byteOrder, BoundedLimit outputLengthLimit, CdrLimits limits) {
    this.byteOrder = Objects.requireNonNull(byteOrder, "byteOrder");
    this.outputLengthLimit = Objects.requireNonNull(outputLengthLimit, "outputLengthLimit");
    this.limits = Objects.requireNonNull(limits, "limits");
  }

  /** Creates a big-endian primitive writer. */
  public static CdrWriter bigEndian() {
    return new CdrWriter(CdrByteOrder.BIG_ENDIAN);
  }

  /** Creates a big-endian writer with caller-supplied limits. */
  public static CdrWriter bigEndian(CdrLimits limits) {
    return new CdrWriter(CdrByteOrder.BIG_ENDIAN, limits);
  }

  /** Creates a little-endian primitive writer. */
  public static CdrWriter littleEndian() {
    return new CdrWriter(CdrByteOrder.LITTLE_ENDIAN);
  }

  /** Creates a little-endian writer with caller-supplied limits. */
  public static CdrWriter littleEndian(CdrLimits limits) {
    return new CdrWriter(CdrByteOrder.LITTLE_ENDIAN, limits);
  }

  /** Returns the configured byte order. */
  public CdrByteOrder byteOrder() {
    return byteOrder;
  }

  /** Returns the configured bounds for length-bearing CDR values. */
  public CdrLimits limits() {
    return limits;
  }

  /** Returns the next byte offset to be written. */
  public int position() {
    return position;
  }

  /** Writes zero padding until the next offset aligned for the supplied byte boundary. */
  public CdrWriter align(int alignment) {
    prepareAlignedWrite(alignment, 0);
    return this;
  }

  /** Writes a strict CDR boolean value. */
  public CdrWriter writeBoolean(boolean value) {
    return writeUnsigned(1, 1, value ? 1L : 0L);
  }

  /** Writes an unsigned CDR octet value. */
  public CdrWriter writeOctet(int value) {
    requireUnsigned(value, 0xFFL, "octet");
    return writeUnsigned(1, 1, value);
  }

  /** Writes a CDR char value as one octet. */
  public CdrWriter writeChar(char value) {
    if (value > 0x00FF) {
      throw new CdrException(
          CdrDiagnosticCodes.INVALID_CHARACTER,
          "CDR char value must fit in one octet: " + (int) value);
    }
    return writeUnsigned(1, 1, value);
  }

  /** Writes a signed CDR short value. */
  public CdrWriter writeShort(short value) {
    return writeUnsigned(2, 2, value & 0xFFFFL);
  }

  /** Writes an unsigned CDR short value. */
  public CdrWriter writeUnsignedShort(int value) {
    requireUnsigned(value, 0xFFFFL, "unsigned short");
    return writeUnsigned(2, 2, value);
  }

  /** Writes a signed CDR long value. */
  public CdrWriter writeLong(int value) {
    return writeUnsigned(4, 4, value & 0xFFFFFFFFL);
  }

  /** Writes an unsigned CDR long value. */
  public CdrWriter writeUnsignedLong(long value) {
    requireUnsigned(value, 0xFFFFFFFFL, "unsigned long");
    return writeUnsigned(4, 4, value);
  }

  /** Writes a signed CDR long long value. */
  public CdrWriter writeLongLong(long value) {
    return writeUnsigned(8, 8, value);
  }

  /** Writes an unsigned CDR long long value. */
  public CdrWriter writeUnsignedLongLong(BigInteger value) {
    Objects.requireNonNull(value, "value");
    if (value.signum() < 0 || value.compareTo(UNSIGNED_LONG_LONG_LIMIT) >= 0) {
      throw new CdrException(
          CdrDiagnosticCodes.UNSIGNED_VALUE_OUT_OF_RANGE,
          "unsigned long long value must fit in 64 bits: " + value);
    }
    prepareAlignedWrite(8, 8);
    writeBigIntegerBytes(value, 8);
    return this;
  }

  /** Writes a CDR float value. */
  public CdrWriter writeFloat(float value) {
    return writeLong(Float.floatToRawIntBits(value));
  }

  /** Writes a CDR double value. */
  public CdrWriter writeDouble(double value) {
    return writeLongLong(Double.doubleToRawLongBits(value));
  }

  /** Writes a raw 16-octet CDR long double payload. */
  public CdrWriter writeLongDoubleBytes(byte[] value) {
    Objects.requireNonNull(value, "value");
    if (value.length != 16) {
      throw new CdrException(
          CdrDiagnosticCodes.INVALID_LONG_DOUBLE,
          "CDR long double payload must be exactly 16 octets: " + value.length);
    }
    prepareAlignedWrite(8, 16);
    System.arraycopy(value, 0, output, position, value.length);
    position += value.length;
    return this;
  }

  /** Writes a bounded narrow CDR string using one-octet Latin-1 character mapping. */
  public CdrWriter writeString(String value) {
    Objects.requireNonNull(value, "value");
    long length = (long) value.length() + 1L;
    requireBoundedLength(length, limits.stringOctets());
    byte[] bytes = new byte[(int) length];
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      if (character > 0x00FF) {
        throw new CdrException(
            CdrDiagnosticCodes.INVALID_CHARACTER,
            "CDR string character must fit in one octet: " + (int) character);
      }
      bytes[index] = (byte) character;
    }
    writeUnsignedLong(length);
    writeRawBytes(bytes);
    return this;
  }

  /** Writes a bounded CDR sequence length. */
  public CdrWriter writeSequenceLength(int elementCount) {
    validateSequenceLength(elementCount);
    return writeUnsignedLong(elementCount);
  }

  /** Validates a fixed-array element count for generated-code loops. */
  public int validateFixedArrayLength(int elementCount) {
    return validateSequenceLength(elementCount);
  }

  /** Writes a bounded CDR sequence of octets. */
  public CdrWriter writeOctetSequence(byte[] value) {
    Objects.requireNonNull(value, "value");
    writeSequenceLength(value.length);
    writeRawBytes(value);
    return this;
  }

  /** Writes raw octets without a CDR length prefix. */
  public CdrWriter writeOctets(byte[] value) {
    writeRawBytes(Arrays.copyOf(Objects.requireNonNull(value, "value"), value.length));
    return this;
  }

  /** Writes a bounded length-prefixed CDR encapsulation. */
  public CdrWriter writeEncapsulation(CdrEncapsulation encapsulation) {
    Objects.requireNonNull(encapsulation, "encapsulation");
    byte[] bytes = encapsulation.bytes();
    requireBoundedLength(bytes.length, limits.encapsulationOctets());
    writeUnsignedLong(bytes.length);
    writeRawBytes(bytes);
    return this;
  }

  /** Returns a defensive copy of the encoded bytes. */
  public byte[] toByteArray() {
    return Arrays.copyOf(output, position);
  }

  private CdrWriter writeUnsigned(int alignment, int byteCount, long value) {
    prepareAlignedWrite(alignment, byteCount);
    if (byteOrder == CdrByteOrder.BIG_ENDIAN) {
      for (int byteIndex = byteCount - 1; byteIndex >= 0; byteIndex--) {
        output[position++] = (byte) (value >>> (byteIndex * 8));
      }
    } else {
      for (int byteIndex = 0; byteIndex < byteCount; byteIndex++) {
        output[position++] = (byte) (value >>> (byteIndex * 8));
      }
    }
    return this;
  }

  private void writeRawBytes(byte[] bytes) {
    prepareAlignedWrite(1, bytes.length);
    System.arraycopy(bytes, 0, output, position, bytes.length);
    position += bytes.length;
  }

  private void writeBigIntegerBytes(BigInteger value, int byteCount) {
    if (byteOrder == CdrByteOrder.BIG_ENDIAN) {
      for (int byteIndex = byteCount - 1; byteIndex >= 0; byteIndex--) {
        output[position++] = value.shiftRight(byteIndex * 8).byteValue();
      }
    } else {
      for (int byteIndex = 0; byteIndex < byteCount; byteIndex++) {
        output[position++] = value.shiftRight(byteIndex * 8).byteValue();
      }
    }
  }

  private void prepareAlignedWrite(int alignment, int byteCount) {
    int padding = paddingFor(position, alignment);
    long requiredLength = (long) position + padding + byteCount;
    outputLengthLimit.check(requiredLength).ifPresent(CdrWriter::throwOutputLimitExceeded);
    if (requiredLength > Integer.MAX_VALUE) {
      throw new CdrException(
          CdrDiagnosticCodes.OUTPUT_LIMIT_EXCEEDED,
          "CDR output length exceeds Java array limits: " + requiredLength);
    }
    ensureCapacity((int) requiredLength);
    Arrays.fill(output, position, position + padding, (byte) 0);
    position += padding;
  }

  private void ensureCapacity(int requiredLength) {
    if (requiredLength <= output.length) {
      return;
    }
    int newLength = output.length;
    while (newLength < requiredLength) {
      newLength = Math.max(requiredLength, newLength * 2);
    }
    output = Arrays.copyOf(output, newLength);
  }

  private static void requireUnsigned(long value, long maximum, String label) {
    if (value < 0 || value > maximum) {
      throw new CdrException(
          CdrDiagnosticCodes.UNSIGNED_VALUE_OUT_OF_RANGE,
          "CDR " + label + " value out of range: " + value);
    }
  }

  private int validateSequenceLength(int elementCount) {
    if (elementCount < 0) {
      throw new CdrException(
          CdrDiagnosticCodes.INVALID_COLLECTION_SIZE,
          "CDR sequence element count must be nonnegative: " + elementCount);
    }
    requireBoundedLength(elementCount, limits.sequenceElements());
    return elementCount;
  }

  private static void requireBoundedLength(long length, BoundedLimit limit) {
    if (length < 0 || length > 0xFFFF_FFFFL) {
      throw new CdrException(
          CdrDiagnosticCodes.INVALID_LENGTH, "CDR length must fit in unsigned long: " + length);
    }
    limit.check(length).ifPresent(CdrWriter::throwLengthLimitExceeded);
  }

  private static int paddingFor(int position, int alignment) {
    if (alignment <= 0 || Integer.bitCount(alignment) != 1 || alignment > 8) {
      throw new IllegalArgumentException("alignment must be a power of two in 1..8");
    }
    int remainder = position % alignment;
    return remainder == 0 ? 0 : alignment - remainder;
  }

  private static void throwOutputLimitExceeded(LimitViolation violation) {
    throw new CdrException(CdrDiagnosticCodes.OUTPUT_LIMIT_EXCEEDED, violation.message());
  }

  private static void throwLengthLimitExceeded(LimitViolation violation) {
    throw new CdrException(CdrDiagnosticCodes.LENGTH_LIMIT_EXCEEDED, violation.message());
  }
}
