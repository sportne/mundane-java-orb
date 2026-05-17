package io.github.mundanej.mjo.giop;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** GIOP 1.2 reply message. */
public final class GiopReply implements GiopMessage {

  private final GiopHeader header;
  private final long requestId;
  private final GiopReplyStatus replyStatus;
  private final List<GiopServiceContext> serviceContexts;
  private final byte[] body;

  /** Creates a reply message. */
  public GiopReply(
      GiopHeader header,
      long requestId,
      GiopReplyStatus replyStatus,
      List<GiopServiceContext> serviceContexts,
      byte[] body) {
    this.header = GiopModel.requireHeader(header, GiopMessageType.REPLY);
    GiopModel.requireUnsignedLong(requestId, "requestId");
    this.requestId = requestId;
    this.replyStatus = Objects.requireNonNull(replyStatus, "replyStatus");
    this.serviceContexts = List.copyOf(Objects.requireNonNull(serviceContexts, "serviceContexts"));
    this.body = GiopModel.copyBytes(body, "body");
  }

  @Override
  public GiopHeader header() {
    return header;
  }

  public long requestId() {
    return requestId;
  }

  public GiopReplyStatus replyStatus() {
    return replyStatus;
  }

  public List<GiopServiceContext> serviceContexts() {
    return serviceContexts;
  }

  public byte[] body() {
    return Arrays.copyOf(body, body.length);
  }
}
