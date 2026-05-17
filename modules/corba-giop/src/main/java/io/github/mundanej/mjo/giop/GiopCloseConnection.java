package io.github.mundanej.mjo.giop;

/** GIOP close connection message. */
public record GiopCloseConnection(GiopHeader header) implements GiopMessage {

  /** Creates a close connection message. */
  public GiopCloseConnection {
    header = GiopModel.requireHeader(header, GiopMessageType.CLOSE_CONNECTION);
  }
}
