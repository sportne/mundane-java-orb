package io.github.mundanej.mjo.giop;

import java.util.Objects;

/**
 * Fixed GIOP message header.
 *
 * @param version protocol version
 * @param littleEndian whether the body uses little-endian CDR
 * @param moreFragments whether more fragments follow this message
 * @param messageType message kind
 * @param messageSize octets after the fixed 12-octet header
 */
public record GiopHeader(
    GiopVersion version,
    boolean littleEndian,
    boolean moreFragments,
    GiopMessageType messageType,
    int messageSize) {

  /** Creates a validated header value. */
  public GiopHeader {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(messageType, "messageType");
    if (messageSize < 0) {
      throw new GiopException(
          GiopDiagnosticCodes.INVALID_BODY, "GIOP message size must be nonnegative");
    }
  }

  /** Creates a GIOP 1.2 header placeholder for writer-side message values. */
  public static GiopHeader forType(GiopMessageType messageType) {
    return new GiopHeader(GiopVersion.GIOP_1_2, false, false, messageType, 0);
  }

  GiopHeader withMessageSize(int size) {
    return new GiopHeader(version, littleEndian, moreFragments, messageType, size);
  }
}
