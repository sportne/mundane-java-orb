package io.github.mundanej.mjo.ior;

import java.util.Arrays;
import java.util.Objects;

final class IorWire {

  private static final long UNSIGNED_LONG_MAXIMUM = 0xFFFF_FFFFL;
  private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

  private IorWire() {}

  static long requireUnsignedLong(long value, String label) {
    if (value < 0 || value > UNSIGNED_LONG_MAXIMUM) {
      throw new IorException(
          IorDiagnosticCodes.TAG_OUT_OF_RANGE, label + " must fit in unsigned long: " + value);
    }
    return value;
  }

  static int requireUnsignedShort(int value, String label) {
    if (value < 0 || value > 0xFFFF) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_PORT, label + " must fit in unsigned short: " + value);
    }
    return value;
  }

  static String requireNonBlank(String value, String label) {
    Objects.requireNonNull(value, label);
    if (value.isBlank()) {
      throw new IorException(IorDiagnosticCodes.INVALID_OBJECT_URL, label + " must not be blank");
    }
    return value;
  }

  static byte[] copyLimited(byte[] bytes, IorLimits limits, String label) {
    Objects.requireNonNull(bytes, label);
    limits.requireWithin(limits.objectKeyOctets(), bytes.length);
    return Arrays.copyOf(bytes, bytes.length);
  }

  static byte[] decodeHex(String value) {
    Objects.requireNonNull(value, "value");
    if (value.length() % 2 != 0) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_STRINGIFIED_IOR,
          "stringified IOR hex payload must contain an even number of characters");
    }
    byte[] bytes = new byte[value.length() / 2];
    for (int index = 0; index < value.length(); index += 2) {
      int high = hexValue(value.charAt(index));
      int low = hexValue(value.charAt(index + 1));
      bytes[index / 2] = (byte) ((high << 4) | low);
    }
    return bytes;
  }

  static String encodeHex(byte[] bytes) {
    Objects.requireNonNull(bytes, "bytes");
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte current : bytes) {
      builder.append(HEX_DIGITS[(current >>> 4) & 0x0F]);
      builder.append(HEX_DIGITS[current & 0x0F]);
    }
    return builder.toString();
  }

  private static int hexValue(char character) {
    if (character >= '0' && character <= '9') {
      return character - '0';
    }
    if (character >= 'a' && character <= 'f') {
      return character - 'a' + 10;
    }
    if (character >= 'A' && character <= 'F') {
      return character - 'A' + 10;
    }
    throw new IorException(
        IorDiagnosticCodes.INVALID_STRINGIFIED_IOR,
        "stringified IOR contains a non-hex character: " + character);
  }
}
