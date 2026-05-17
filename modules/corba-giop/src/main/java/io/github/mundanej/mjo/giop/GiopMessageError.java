package io.github.mundanej.mjo.giop;

/** GIOP message error message. */
public record GiopMessageError(GiopHeader header) implements GiopMessage {

  /** Creates a message error message. */
  public GiopMessageError {
    header = GiopModel.requireHeader(header, GiopMessageType.MESSAGE_ERROR);
  }
}
