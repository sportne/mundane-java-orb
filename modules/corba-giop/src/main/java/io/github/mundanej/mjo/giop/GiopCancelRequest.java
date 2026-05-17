package io.github.mundanej.mjo.giop;

/** GIOP cancel request message. */
public record GiopCancelRequest(GiopHeader header, long requestId) implements GiopMessage {

  /** Creates a cancel request message. */
  public GiopCancelRequest {
    header = GiopModel.requireHeader(header, GiopMessageType.CANCEL_REQUEST);
    GiopModel.requireUnsignedLong(requestId, "requestId");
  }
}
