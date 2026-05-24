package io.github.mundanej.mjo.giop;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrWriter;
import java.util.List;
import java.util.Objects;

/** Writes bounded GIOP 1.2 messages to complete in-memory byte arrays. */
public final class GiopMessageWriter {

  private static final int HEADER_LENGTH = 12;
  private static final int MAGIC_G = 'G';
  private static final int MAGIC_I = 'I';
  private static final int MAGIC_O = 'O';
  private static final int MAGIC_P = 'P';
  private static final int LITTLE_ENDIAN_FLAG = 0x01;
  private static final int MORE_FRAGMENTS_FLAG = 0x02;

  private final GiopLimits limits;

  public GiopMessageWriter() {
    this(GiopLimits.defaults());
  }

  public GiopMessageWriter(GiopLimits limits) {
    this.limits = Objects.requireNonNull(limits, "limits");
  }

  public byte[] write(GiopMessage message) {
    Objects.requireNonNull(message, "message");
    byte[] body = writeBody(message);
    limits.check(limits.bodyOctets(), body.length);
    long messageLength = HEADER_LENGTH + (long) body.length;
    limits.check(limits.messageOctets(), messageLength);
    byte[] output = new byte[Math.toIntExact(messageLength)];
    writeHeader(message.header().withMessageSize(body.length), output);
    System.arraycopy(body, 0, output, HEADER_LENGTH, body.length);
    return output;
  }

  private byte[] writeBody(GiopMessage message) {
    if (message instanceof GiopRequest request) {
      return writeRequest(request);
    }
    if (message instanceof GiopReply reply) {
      return writeReply(reply);
    }
    if (message instanceof GiopCancelRequest cancelRequest) {
      return writeCancelRequest(cancelRequest);
    }
    if (message instanceof GiopLocateRequest locateRequest) {
      return writeLocateRequest(locateRequest);
    }
    if (message instanceof GiopLocateReply locateReply) {
      return writeLocateReply(locateReply);
    }
    if (message instanceof GiopCloseConnection || message instanceof GiopMessageError) {
      return new byte[0];
    }
    if (message instanceof GiopFragment fragment) {
      return writeFragment(fragment);
    }
    throw new GiopException(
        GiopDiagnosticCodes.UNSUPPORTED_BODY,
        "Unsupported GIOP message implementation: " + message.getClass().getName());
  }

  private byte[] writeRequest(GiopRequest request) {
    CdrWriter writer = bodyWriter(request.header());
    writer.writeUnsignedLong(request.requestId());
    writer.writeOctet(request.responseFlags());
    writer.writeOctets(new byte[3]);
    writeTargetAddress(writer, request.targetAddress());
    writer.writeString(request.operation());
    writeServiceContexts(writer, request.serviceContexts());
    writer.writeOctets(request.body());
    return writer.toByteArray();
  }

  private byte[] writeReply(GiopReply reply) {
    CdrWriter writer = bodyWriter(reply.header());
    writer.writeUnsignedLong(reply.requestId());
    writer.writeUnsignedLong(reply.replyStatus().id());
    writeServiceContexts(writer, reply.serviceContexts());
    writer.writeOctets(reply.body());
    return writer.toByteArray();
  }

  private byte[] writeCancelRequest(GiopCancelRequest cancelRequest) {
    CdrWriter writer = bodyWriter(cancelRequest.header());
    writer.writeUnsignedLong(cancelRequest.requestId());
    return writer.toByteArray();
  }

  private byte[] writeLocateRequest(GiopLocateRequest locateRequest) {
    CdrWriter writer = bodyWriter(locateRequest.header());
    writer.writeUnsignedLong(locateRequest.requestId());
    writeTargetAddress(writer, locateRequest.targetAddress());
    return writer.toByteArray();
  }

  private byte[] writeLocateReply(GiopLocateReply locateReply) {
    CdrWriter writer = bodyWriter(locateReply.header());
    writer.writeUnsignedLong(locateReply.requestId());
    writer.writeUnsignedLong(locateReply.locateStatus().id());
    writer.writeOctets(locateReply.body());
    return writer.toByteArray();
  }

  private byte[] writeFragment(GiopFragment fragment) {
    CdrWriter writer = bodyWriter(fragment.header());
    writer.writeUnsignedLong(fragment.requestId());
    writer.writeOctets(fragment.fragmentPayload());
    return writer.toByteArray();
  }

  private void writeTargetAddress(CdrWriter writer, GiopTargetAddress targetAddress) {
    writer.writeShort(targetAddress.discriminator());
    switch (targetAddress.discriminator()) {
      case GiopTargetAddress.KEY_ADDR -> writer.writeOctetSequence(targetAddress.objectKey());
      case GiopTargetAddress.PROFILE_ADDR -> targetAddress.profile().writeTo(writer);
      case GiopTargetAddress.REFERENCE_ADDR -> {
        writer.writeUnsignedLong(targetAddress.selectedProfileIndex());
        targetAddress.ior().writeTo(writer);
      }
      default ->
          throw new GiopException(
              GiopDiagnosticCodes.UNSUPPORTED_BODY,
              "Unsupported GIOP target-address discriminator: " + targetAddress.discriminator());
    }
  }

  private void writeServiceContexts(CdrWriter writer, List<GiopServiceContext> serviceContexts) {
    limits.check(limits.serviceContextCount(), serviceContexts.size());
    writer.writeSequenceLength(serviceContexts.size());
    for (GiopServiceContext serviceContext : serviceContexts) {
      writer.writeUnsignedLong(serviceContext.contextId());
      byte[] contextData = serviceContext.contextData();
      limits.check(limits.serviceContextDataOctets(), contextData.length);
      writer.writeSequenceLength(contextData.length);
      writer.writeOctets(contextData);
    }
  }

  private CdrWriter bodyWriter(GiopHeader header) {
    return new CdrWriter(
        header.littleEndian() ? CdrByteOrder.LITTLE_ENDIAN : CdrByteOrder.BIG_ENDIAN,
        limits.bodyOctets());
  }

  private static void writeHeader(GiopHeader header, byte[] output) {
    output[0] = MAGIC_G;
    output[1] = MAGIC_I;
    output[2] = MAGIC_O;
    output[3] = MAGIC_P;
    output[4] = (byte) header.version().major();
    output[5] = (byte) header.version().minor();
    output[6] =
        (byte)
            ((header.littleEndian() ? LITTLE_ENDIAN_FLAG : 0)
                | (header.moreFragments() ? MORE_FRAGMENTS_FLAG : 0));
    output[7] = (byte) header.messageType().id();
    writeMessageSize(header.messageSize(), header.littleEndian(), output);
  }

  private static void writeMessageSize(int messageSize, boolean littleEndian, byte[] output) {
    if (littleEndian) {
      output[8] = (byte) messageSize;
      output[9] = (byte) (messageSize >>> 8);
      output[10] = (byte) (messageSize >>> 16);
      output[11] = (byte) (messageSize >>> 24);
      return;
    }
    output[8] = (byte) (messageSize >>> 24);
    output[9] = (byte) (messageSize >>> 16);
    output[10] = (byte) (messageSize >>> 8);
    output[11] = (byte) messageSize;
  }
}
