package io.github.mundanej.mjo.giop;

/** GIOP reply status values. */
public enum GiopReplyStatus {
  NO_EXCEPTION(0),
  USER_EXCEPTION(1),
  SYSTEM_EXCEPTION(2),
  LOCATION_FORWARD(3),
  LOCATION_FORWARD_PERM(4),
  NEEDS_ADDRESSING_MODE(5);

  private final int id;

  GiopReplyStatus(int id) {
    this.id = id;
  }

  /** Returns the CDR enum ordinal used on the wire. */
  public int id() {
    return id;
  }

  static GiopReplyStatus fromId(long id) {
    for (GiopReplyStatus status : values()) {
      if (status.id == id) {
        return status;
      }
    }
    throw new GiopException(GiopDiagnosticCodes.INVALID_BODY, "Unknown GIOP reply status: " + id);
  }
}
