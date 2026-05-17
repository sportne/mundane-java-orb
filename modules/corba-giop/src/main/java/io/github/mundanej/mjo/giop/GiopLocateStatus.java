package io.github.mundanej.mjo.giop;

/** GIOP locate reply status values. */
public enum GiopLocateStatus {
  UNKNOWN_OBJECT(0),
  OBJECT_HERE(1),
  OBJECT_FORWARD(2),
  OBJECT_FORWARD_PERM(3),
  LOC_SYSTEM_EXCEPTION(4),
  LOC_NEEDS_ADDRESSING_MODE(5);

  private final int id;

  GiopLocateStatus(int id) {
    this.id = id;
  }

  /** Returns the CDR enum ordinal used on the wire. */
  public int id() {
    return id;
  }

  static GiopLocateStatus fromId(long id) {
    for (GiopLocateStatus status : values()) {
      if (status.id == id) {
        return status;
      }
    }
    throw new GiopException(GiopDiagnosticCodes.INVALID_BODY, "Unknown GIOP locate status: " + id);
  }
}
