package io.github.mundanej.mjo.cdr;

import java.util.Arrays;
import java.util.Objects;

/** Immutable CDR encapsulation payload, including its leading byte-order marker. */
public final class CdrEncapsulation {

  private final CdrByteOrder byteOrder;
  private final byte[] bytes;

  /** Creates an encapsulation from encoded bytes that include the byte-order marker. */
  public CdrEncapsulation(CdrByteOrder byteOrder, byte[] bytes) {
    this.byteOrder = Objects.requireNonNull(byteOrder, "byteOrder");
    Objects.requireNonNull(bytes, "bytes");
    if (bytes.length == 0) {
      throw new CdrException(
          CdrDiagnosticCodes.INVALID_LENGTH, "CDR encapsulation must include a byte-order marker");
    }
    CdrByteOrder markerOrder = byteOrderFromMarker(bytes[0] & 0xFF);
    if (markerOrder != byteOrder) {
      throw new CdrException(
          CdrDiagnosticCodes.INVALID_ENCAPSULATION_BYTE_ORDER,
          "CDR encapsulation marker does not match supplied byte order");
    }
    this.bytes = Arrays.copyOf(bytes, bytes.length);
  }

  /** Creates an encapsulation from nested body bytes, adding the byte-order marker. */
  public static CdrEncapsulation of(CdrByteOrder byteOrder, byte[] bodyBytes) {
    Objects.requireNonNull(byteOrder, "byteOrder");
    Objects.requireNonNull(bodyBytes, "bodyBytes");
    byte[] bytes = new byte[bodyBytes.length + 1];
    bytes[0] = markerFor(byteOrder);
    System.arraycopy(bodyBytes, 0, bytes, 1, bodyBytes.length);
    return new CdrEncapsulation(byteOrder, bytes);
  }

  /** Creates an encapsulation from encoded bytes that include the byte-order marker. */
  public static CdrEncapsulation fromBytes(byte[] bytes) {
    Objects.requireNonNull(bytes, "bytes");
    if (bytes.length == 0) {
      throw new CdrException(
          CdrDiagnosticCodes.INVALID_LENGTH, "CDR encapsulation must include a byte-order marker");
    }
    return new CdrEncapsulation(byteOrderFromMarker(bytes[0] & 0xFF), bytes);
  }

  /** Returns the byte order declared by the first payload octet. */
  public CdrByteOrder byteOrder() {
    return byteOrder;
  }

  /** Returns a defensive copy of the encoded encapsulation bytes. */
  public byte[] bytes() {
    return Arrays.copyOf(bytes, bytes.length);
  }

  /** Creates a nested reader positioned after the byte-order marker. */
  public CdrReader reader() {
    return reader(CdrLimits.defaults());
  }

  /**
   * Creates a nested reader with caller-supplied limits, positioned after the byte-order marker.
   */
  public CdrReader reader(CdrLimits limits) {
    CdrReader reader = new CdrReader(byteOrder, bytes, limits);
    reader.readOctet();
    return reader;
  }

  /** Encapsulations compare by byte order and byte contents. */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof CdrEncapsulation that)) {
      return false;
    }
    return byteOrder == that.byteOrder && Arrays.equals(bytes, that.bytes);
  }

  /** Returns a byte-content hash code. */
  @Override
  public int hashCode() {
    return 31 * byteOrder.hashCode() + Arrays.hashCode(bytes);
  }

  /** Returns a stable summary without dumping payload bytes. */
  @Override
  public String toString() {
    return "CdrEncapsulation[byteOrder=" + byteOrder + ", byteLength=" + bytes.length + "]";
  }

  private static CdrByteOrder byteOrderFromMarker(int marker) {
    if (marker == 0) {
      return CdrByteOrder.BIG_ENDIAN;
    }
    if (marker == 1) {
      return CdrByteOrder.LITTLE_ENDIAN;
    }
    throw new CdrException(
        CdrDiagnosticCodes.INVALID_ENCAPSULATION_BYTE_ORDER,
        "CDR encapsulation byte-order marker must be 0 or 1: " + marker);
  }

  private static byte markerFor(CdrByteOrder byteOrder) {
    return switch (byteOrder) {
      case BIG_ENDIAN -> 0;
      case LITTLE_ENDIAN -> 1;
    };
  }
}
