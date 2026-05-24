package org.omg.PortableServer;

/** Enum-like POA manager state. */
public final class State {

  public static final int _HOLDING = 0;
  public static final int _ACTIVE = 1;
  public static final int _DISCARDING = 2;
  public static final int _INACTIVE = 3;

  public static final State HOLDING = new State(_HOLDING);
  public static final State ACTIVE = new State(_ACTIVE);
  public static final State DISCARDING = new State(_DISCARDING);
  public static final State INACTIVE = new State(_INACTIVE);

  private final int value;

  private State(int value) {
    this.value = value;
  }

  /** Returns the integer constant value. */
  public int value() {
    return value;
  }
}
