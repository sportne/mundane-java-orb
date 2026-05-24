package io.github.mundanej.mjo.giop;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import java.util.Arrays;

/** Deterministic GIOP user-exception reply body. */
public final class GiopUserExceptionBody {

  private final String repositoryId;
  private final byte[] payload;

  /** Creates a body with an encoded exception repository ID and payload. */
  public GiopUserExceptionBody(String repositoryId, byte[] payload) {
    this.repositoryId = GiopModel.requireNonBlank(repositoryId, "repositoryId");
    this.payload = GiopModel.copyBytes(payload, "payload");
  }

  /** Decodes a user-exception body. */
  public static GiopUserExceptionBody fromBytes(CdrByteOrder byteOrder, byte[] body) {
    CdrReader reader = new CdrReader(byteOrder, body);
    String repositoryId = reader.readString();
    return new GiopUserExceptionBody(repositoryId, reader.readOctets(reader.remaining()));
  }

  /** Encodes this body in the supplied byte order. */
  public byte[] toBytes(CdrByteOrder byteOrder) {
    CdrWriter writer = new CdrWriter(byteOrder);
    writer.writeString(repositoryId);
    writer.writeOctets(payload);
    return writer.toByteArray();
  }

  public String repositoryId() {
    return repositoryId;
  }

  public byte[] payload() {
    return Arrays.copyOf(payload, payload.length);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof GiopUserExceptionBody that
        && repositoryId.equals(that.repositoryId)
        && Arrays.equals(payload, that.payload);
  }

  @Override
  public int hashCode() {
    return 31 * repositoryId.hashCode() + Arrays.hashCode(payload);
  }
}
