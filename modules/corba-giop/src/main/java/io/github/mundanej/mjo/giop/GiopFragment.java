package io.github.mundanej.mjo.giop;

import java.util.Arrays;

/** GIOP 1.2 fragment message with opaque fragment payload. */
public final class GiopFragment implements GiopMessage {

  private final GiopHeader header;
  private final long requestId;
  private final byte[] fragmentPayload;

  /** Creates a fragment message. */
  public GiopFragment(GiopHeader header, long requestId, byte[] fragmentPayload) {
    this.header = GiopModel.requireHeader(header, GiopMessageType.FRAGMENT);
    GiopModel.requireUnsignedLong(requestId, "requestId");
    this.requestId = requestId;
    this.fragmentPayload = GiopModel.copyBytes(fragmentPayload, "fragmentPayload");
  }

  @Override
  public GiopHeader header() {
    return header;
  }

  public long requestId() {
    return requestId;
  }

  public byte[] fragmentPayload() {
    return Arrays.copyOf(fragmentPayload, fragmentPayload.length);
  }
}
