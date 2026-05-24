package io.github.mundanej.mjo.giop;

import java.util.Objects;

/** GIOP locate request message with TargetAddress support. */
public final class GiopLocateRequest implements GiopMessage {

  private final GiopHeader header;
  private final long requestId;
  private final GiopTargetAddress targetAddress;

  /** Creates a locate request message. */
  public GiopLocateRequest(GiopHeader header, long requestId, byte[] objectKey) {
    this(header, requestId, GiopTargetAddress.keyAddr(objectKey));
  }

  /** Creates a locate request message. */
  public GiopLocateRequest(GiopHeader header, long requestId, GiopTargetAddress targetAddress) {
    this.header = GiopModel.requireHeader(header, GiopMessageType.LOCATE_REQUEST);
    GiopModel.requireUnsignedLong(requestId, "requestId");
    this.requestId = requestId;
    this.targetAddress = Objects.requireNonNull(targetAddress, "targetAddress");
  }

  @Override
  public GiopHeader header() {
    return header;
  }

  public long requestId() {
    return requestId;
  }

  public byte[] objectKey() {
    return targetAddress.objectKey();
  }

  public GiopTargetAddress targetAddress() {
    return targetAddress;
  }
}
