package io.github.mundanej.mjo.giop;

/** CompletionStatus values encoded in GIOP system-exception reply bodies. */
public enum GiopCompletionStatus {
  COMPLETED_YES(0),
  COMPLETED_NO(1),
  COMPLETED_MAYBE(2);

  private final long id;

  GiopCompletionStatus(long id) {
    this.id = id;
  }

  /** Returns the wire unsigned-long value. */
  public long id() {
    return id;
  }

  /** Maps a wire value to a completion status. */
  public static GiopCompletionStatus fromId(long id) {
    for (GiopCompletionStatus status : values()) {
      if (status.id == id) {
        return status;
      }
    }
    throw new GiopException(GiopDiagnosticCodes.INVALID_BODY, "Unknown completion status: " + id);
  }
}
