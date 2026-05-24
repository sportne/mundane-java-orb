package org.omg.CORBA;

/** Checked bounds exception used by legacy list APIs. */
public final class Bounds extends UserException {

  private static final long serialVersionUID = 1L;

  /** Creates a bounds exception. */
  public Bounds() {
    super();
  }

  /** Creates a bounds exception with a message. */
  public Bounds(String message) {
    super(message);
  }
}
