package io.github.mundanej.mjo.giop;

/** GIOP message type octets. */
public enum GiopMessageType {
  REQUEST(0),
  REPLY(1),
  CANCEL_REQUEST(2),
  LOCATE_REQUEST(3),
  LOCATE_REPLY(4),
  CLOSE_CONNECTION(5),
  MESSAGE_ERROR(6),
  FRAGMENT(7);

  private final int id;

  GiopMessageType(int id) {
    this.id = id;
  }

  /** Returns the GIOP message type octet. */
  public int id() {
    return id;
  }

  static GiopMessageType fromId(int id) {
    for (GiopMessageType type : values()) {
      if (type.id == id) {
        return type;
      }
    }
    throw new GiopException(
        GiopDiagnosticCodes.UNKNOWN_MESSAGE_TYPE, "Unknown GIOP message type: " + id);
  }
}
