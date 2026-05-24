package io.github.mundanej.mjo.giop;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import java.util.Objects;

/** Deterministic GIOP system-exception reply body. */
public record GiopSystemExceptionBody(
    String repositoryId, long minorCodeValue, GiopCompletionStatus completionStatus) {

  /** Creates a validated body. */
  public GiopSystemExceptionBody {
    repositoryId = GiopModel.requireNonBlank(repositoryId, "repositoryId");
    GiopModel.requireUnsignedLong(minorCodeValue, "minorCodeValue");
    Objects.requireNonNull(completionStatus, "completionStatus");
  }

  /** Encodes this body in the supplied byte order. */
  public byte[] toBytes(CdrByteOrder byteOrder) {
    CdrWriter writer = new CdrWriter(byteOrder);
    writer.writeString(repositoryId);
    writer.writeUnsignedLong(minorCodeValue);
    writer.writeUnsignedLong(completionStatus.id());
    return writer.toByteArray();
  }

  /** Decodes a system-exception body. */
  public static GiopSystemExceptionBody fromBytes(CdrByteOrder byteOrder, byte[] body) {
    CdrReader reader = new CdrReader(byteOrder, body);
    GiopSystemExceptionBody result =
        new GiopSystemExceptionBody(
            reader.readString(),
            reader.readUnsignedLong(),
            GiopCompletionStatus.fromId(reader.readUnsignedLong()));
    requireConsumed(reader);
    return result;
  }

  private static void requireConsumed(CdrReader reader) {
    if (reader.remaining() != 0) {
      throw new GiopException(
          GiopDiagnosticCodes.INVALID_BODY,
          "system-exception body has trailing octets: " + reader.remaining());
    }
  }
}
