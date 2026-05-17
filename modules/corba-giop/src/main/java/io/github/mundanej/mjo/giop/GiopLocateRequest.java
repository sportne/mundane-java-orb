package io.github.mundanej.mjo.giop;

import java.util.Arrays;

/** GIOP locate request message with KeyAddr target support. */
public final class GiopLocateRequest implements GiopMessage {

  private final GiopHeader header;
  private final long requestId;
  private final byte[] objectKey;

  /** Creates a locate request message. */
  public GiopLocateRequest(GiopHeader header, long requestId, byte[] objectKey) {
    this.header = GiopModel.requireHeader(header, GiopMessageType.LOCATE_REQUEST);
    GiopModel.requireUnsignedLong(requestId, "requestId");
    this.requestId = requestId;
    this.objectKey = GiopModel.copyBytes(objectKey, "objectKey");
  }

  @Override
  public GiopHeader header() {
    return header;
  }

  public long requestId() {
    return requestId;
  }

  public byte[] objectKey() {
    return Arrays.copyOf(objectKey, objectKey.length);
  }
}
