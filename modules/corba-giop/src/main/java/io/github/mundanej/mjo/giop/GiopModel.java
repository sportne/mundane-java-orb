package io.github.mundanej.mjo.giop;

import java.util.Arrays;
import java.util.Objects;

final class GiopModel {

  private GiopModel() {}

  static GiopHeader requireHeader(GiopHeader header, GiopMessageType expectedType) {
    Objects.requireNonNull(header, "header");
    if (header.version() != GiopVersion.GIOP_1_2) {
      throw new GiopException(
          GiopDiagnosticCodes.UNSUPPORTED_VERSION,
          "Only GIOP 1.2 headers are supported by this slice");
    }
    if (header.messageType() != expectedType) {
      throw new GiopException(
          GiopDiagnosticCodes.INVALID_BODY,
          "Expected " + expectedType + " header, got " + header.messageType());
    }
    return header;
  }

  static void requireUnsignedLong(long value, String name) {
    if (value < 0 || value > 0xFFFF_FFFFL) {
      throw new GiopException(
          GiopDiagnosticCodes.INVALID_BODY, name + " must fit in unsigned long: " + value);
    }
  }

  static void requireUnsignedOctet(int value, String name) {
    if (value < 0 || value > 0xFF) {
      throw new GiopException(
          GiopDiagnosticCodes.INVALID_BODY, name + " must fit in octet: " + value);
    }
  }

  static byte[] copyBytes(byte[] bytes, String name) {
    return Arrays.copyOf(Objects.requireNonNull(bytes, name), bytes.length);
  }

  static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new GiopException(GiopDiagnosticCodes.INVALID_BODY, name + " must not be blank");
    }
    return value;
  }
}
