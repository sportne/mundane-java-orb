package io.github.mundanej.mjo.giop;

import java.util.Arrays;
import java.util.Objects;

/** GIOP locate reply message. */
public final class GiopLocateReply implements GiopMessage {

  private final GiopHeader header;
  private final long requestId;
  private final GiopLocateStatus locateStatus;
  private final byte[] body;

  /** Creates a locate reply message. */
  public GiopLocateReply(
      GiopHeader header, long requestId, GiopLocateStatus locateStatus, byte[] body) {
    this.header = GiopModel.requireHeader(header, GiopMessageType.LOCATE_REPLY);
    GiopModel.requireUnsignedLong(requestId, "requestId");
    this.requestId = requestId;
    this.locateStatus = Objects.requireNonNull(locateStatus, "locateStatus");
    this.body = GiopModel.copyBytes(body, "body");
  }

  @Override
  public GiopHeader header() {
    return header;
  }

  public long requestId() {
    return requestId;
  }

  public GiopLocateStatus locateStatus() {
    return locateStatus;
  }

  public byte[] body() {
    return Arrays.copyOf(body, body.length);
  }
}
