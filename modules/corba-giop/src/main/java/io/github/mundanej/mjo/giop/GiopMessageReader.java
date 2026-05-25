package io.github.mundanej.mjo.giop;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrException;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.IorLimits;
import io.github.mundanej.mjo.ior.TaggedProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Reads bounded GIOP 1.2 messages from complete in-memory byte arrays. */
public final class GiopMessageReader {

  private static final int HEADER_LENGTH = 12;
  private static final int MAGIC_G = 'G';
  private static final int MAGIC_I = 'I';
  private static final int MAGIC_O = 'O';
  private static final int MAGIC_P = 'P';
  private static final int LITTLE_ENDIAN_FLAG = 0x01;
  private static final int MORE_FRAGMENTS_FLAG = 0x02;
  private static final int SUPPORTED_FLAGS = LITTLE_ENDIAN_FLAG | MORE_FRAGMENTS_FLAG;

  private final GiopLimits limits;

  public GiopMessageReader() {
    this(GiopLimits.defaults());
  }

  public GiopMessageReader(GiopLimits limits) {
    this.limits = Objects.requireNonNull(limits, "limits");
  }

  public GiopMessage read(byte[] messageBytes) {
    Objects.requireNonNull(messageBytes, "messageBytes");
    if (messageBytes.length < HEADER_LENGTH) {
      throw new GiopException(
          GiopDiagnosticCodes.TRUNCATED_MESSAGE,
          "GIOP message header is truncated: " + messageBytes.length + " octets");
    }
    requireMagic(messageBytes);
    GiopVersion version = GiopVersion.fromOctets(messageBytes[4] & 0xff, messageBytes[5] & 0xff);
    int flags = messageBytes[6] & 0xff;
    if ((flags & ~SUPPORTED_FLAGS) != 0) {
      throw new GiopException(
          GiopDiagnosticCodes.INVALID_FLAGS, "GIOP 1.2 flags contain reserved bits: " + flags);
    }
    GiopMessageType messageType = GiopMessageType.fromId(messageBytes[7] & 0xff);
    boolean littleEndian = (flags & LITTLE_ENDIAN_FLAG) != 0;
    boolean moreFragments = (flags & MORE_FRAGMENTS_FLAG) != 0;
    long bodySize = readUnsignedLongHeader(messageBytes, littleEndian);
    if (bodySize > Integer.MAX_VALUE) {
      throw new GiopException(
          GiopDiagnosticCodes.MESSAGE_SIZE_MISMATCH,
          "GIOP message body is too large for an in-memory message: " + bodySize);
    }
    long expectedLength = HEADER_LENGTH + bodySize;
    if (messageBytes.length < expectedLength) {
      throw new GiopException(
          GiopDiagnosticCodes.TRUNCATED_MESSAGE,
          "GIOP message body is truncated: expected "
              + expectedLength
              + " octets but found "
              + messageBytes.length);
    }
    if (messageBytes.length > expectedLength) {
      throw new GiopException(
          GiopDiagnosticCodes.MESSAGE_SIZE_MISMATCH,
          "GIOP message has trailing octets beyond declared size: expected "
              + expectedLength
              + " octets but found "
              + messageBytes.length);
    }
    limits.check(limits.messageOctets(), messageBytes.length);
    limits.check(limits.bodyOctets(), bodySize);
    GiopHeader header =
        new GiopHeader(
            version, littleEndian, moreFragments, messageType, Math.toIntExact(bodySize));
    byte[] body = Arrays.copyOfRange(messageBytes, HEADER_LENGTH, messageBytes.length);
    try {
      return parseBody(header, body);
    } catch (CdrException ex) {
      throw new GiopException(
          GiopDiagnosticCodes.INVALID_BODY,
          "Invalid GIOP " + messageType + " body: " + ex.getMessage());
    }
  }

  private GiopMessage parseBody(GiopHeader header, byte[] body) {
    CdrReader reader = new CdrReader(bodyByteOrder(header), body);
    return switch (header.messageType()) {
      case REQUEST -> readRequest(header, reader);
      case REPLY -> readReply(header, reader);
      case CANCEL_REQUEST -> readCancelRequest(header, reader);
      case LOCATE_REQUEST -> readLocateRequest(header, reader);
      case LOCATE_REPLY -> readLocateReply(header, reader);
      case CLOSE_CONNECTION -> readCloseConnection(header, reader);
      case MESSAGE_ERROR -> readMessageError(header, reader);
      case FRAGMENT -> readFragment(header, reader);
    };
  }

  private GiopRequest readRequest(GiopHeader header, CdrReader reader) {
    long requestId = reader.readUnsignedLong();
    int responseFlags = reader.readOctet();
    byte[] reserved = reader.readOctets(3);
    requireZeroReserved(reserved);
    GiopTargetAddress targetAddress = readTargetAddress(reader);
    String operation = reader.readString();
    List<GiopServiceContext> serviceContexts = readServiceContexts(reader);
    alignOperationBody(reader);
    byte[] body = reader.readOctets(reader.remaining());
    return new GiopRequest(
        header, requestId, responseFlags, targetAddress, operation, serviceContexts, body);
  }

  private GiopReply readReply(GiopHeader header, CdrReader reader) {
    long requestId = reader.readUnsignedLong();
    GiopReplyStatus replyStatus = GiopReplyStatus.fromId(reader.readUnsignedLong());
    List<GiopServiceContext> serviceContexts = readServiceContexts(reader);
    alignOperationBody(reader);
    byte[] body = reader.readOctets(reader.remaining());
    return new GiopReply(header, requestId, replyStatus, serviceContexts, body);
  }

  private GiopCancelRequest readCancelRequest(GiopHeader header, CdrReader reader) {
    long requestId = reader.readUnsignedLong();
    requireFullyConsumed(reader, header.messageType());
    return new GiopCancelRequest(header, requestId);
  }

  private GiopLocateRequest readLocateRequest(GiopHeader header, CdrReader reader) {
    long requestId = reader.readUnsignedLong();
    GiopTargetAddress targetAddress = readTargetAddress(reader);
    requireFullyConsumed(reader, header.messageType());
    return new GiopLocateRequest(header, requestId, targetAddress);
  }

  private GiopLocateReply readLocateReply(GiopHeader header, CdrReader reader) {
    long requestId = reader.readUnsignedLong();
    GiopLocateStatus locateStatus = GiopLocateStatus.fromId(reader.readUnsignedLong());
    byte[] body = reader.readOctets(reader.remaining());
    return new GiopLocateReply(header, requestId, locateStatus, body);
  }

  private GiopCloseConnection readCloseConnection(GiopHeader header, CdrReader reader) {
    requireFullyConsumed(reader, header.messageType());
    return new GiopCloseConnection(header);
  }

  private GiopMessageError readMessageError(GiopHeader header, CdrReader reader) {
    requireFullyConsumed(reader, header.messageType());
    return new GiopMessageError(header);
  }

  private GiopFragment readFragment(GiopHeader header, CdrReader reader) {
    long requestId = reader.readUnsignedLong();
    byte[] fragmentPayload = reader.readOctets(reader.remaining());
    return new GiopFragment(header, requestId, fragmentPayload);
  }

  private GiopTargetAddress readTargetAddress(CdrReader reader) {
    short discriminator = reader.readShort();
    return switch (discriminator) {
      case GiopTargetAddress.KEY_ADDR -> GiopTargetAddress.keyAddr(reader.readOctetSequence());
      case GiopTargetAddress.PROFILE_ADDR ->
          GiopTargetAddress.profileAddr(TaggedProfile.readFrom(reader, IorLimits.defaults()));
      case GiopTargetAddress.REFERENCE_ADDR ->
          GiopTargetAddress.referenceAddr(
              reader.readUnsignedLong(), Ior.readFrom(reader, IorLimits.defaults()));
      default ->
          throw new GiopException(
              GiopDiagnosticCodes.UNSUPPORTED_BODY,
              "Unsupported GIOP target-address discriminator: " + discriminator);
    };
  }

  private static void alignOperationBody(CdrReader reader) {
    if (reader.remaining() > 0) {
      int padding = paddingForMessageOffset(reader.position(), 8);
      if (padding > 0) {
        reader.readOctets(padding);
      }
    }
  }

  private static int paddingForMessageOffset(int bodyPosition, int alignment) {
    int remainder = (HEADER_LENGTH + bodyPosition) % alignment;
    return remainder == 0 ? 0 : alignment - remainder;
  }

  private List<GiopServiceContext> readServiceContexts(CdrReader reader) {
    int count = reader.readSequenceLength();
    limits.check(limits.serviceContextCount(), count);
    List<GiopServiceContext> serviceContexts = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      long contextId = reader.readUnsignedLong();
      int contextDataLength = reader.readSequenceLength();
      limits.check(limits.serviceContextDataOctets(), contextDataLength);
      serviceContexts.add(new GiopServiceContext(contextId, reader.readOctets(contextDataLength)));
    }
    return List.copyOf(serviceContexts);
  }

  private static CdrByteOrder bodyByteOrder(GiopHeader header) {
    return header.littleEndian() ? CdrByteOrder.LITTLE_ENDIAN : CdrByteOrder.BIG_ENDIAN;
  }

  private static void requireMagic(byte[] messageBytes) {
    if ((messageBytes[0] & 0xff) != MAGIC_G
        || (messageBytes[1] & 0xff) != MAGIC_I
        || (messageBytes[2] & 0xff) != MAGIC_O
        || (messageBytes[3] & 0xff) != MAGIC_P) {
      throw new GiopException(GiopDiagnosticCodes.INVALID_MAGIC, "GIOP magic header is invalid");
    }
  }

  private static long readUnsignedLongHeader(byte[] messageBytes, boolean littleEndian) {
    int first = messageBytes[8] & 0xff;
    int second = messageBytes[9] & 0xff;
    int third = messageBytes[10] & 0xff;
    int fourth = messageBytes[11] & 0xff;
    if (littleEndian) {
      return ((long) fourth << 24) | ((long) third << 16) | ((long) second << 8) | first;
    }
    return ((long) first << 24) | ((long) second << 16) | ((long) third << 8) | fourth;
  }

  private static void requireZeroReserved(byte[] reserved) {
    for (byte value : reserved) {
      if (value != 0) {
        throw new GiopException(
            GiopDiagnosticCodes.INVALID_BODY,
            "GIOP Request reserved response flag octets must be zero");
      }
    }
  }

  private static void requireFullyConsumed(CdrReader reader, GiopMessageType messageType) {
    if (reader.remaining() != 0) {
      throw new GiopException(
          GiopDiagnosticCodes.INVALID_BODY,
          "GIOP " + messageType + " body has trailing octets: " + reader.remaining());
    }
  }
}
