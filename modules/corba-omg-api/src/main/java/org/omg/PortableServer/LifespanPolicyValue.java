package org.omg.PortableServer;

/** Enum-like POA lifespan policy value. */
public final class LifespanPolicyValue {

  public static final int _TRANSIENT = 0;
  public static final int _PERSISTENT = 1;

  public static final LifespanPolicyValue TRANSIENT = new LifespanPolicyValue(_TRANSIENT);
  public static final LifespanPolicyValue PERSISTENT = new LifespanPolicyValue(_PERSISTENT);

  private final int value;

  private LifespanPolicyValue(int value) {
    this.value = value;
  }

  /** Returns the integer constant value. */
  public int value() {
    return value;
  }
}
