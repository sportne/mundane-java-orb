package org.omg.PortableServer;

/** Enum-like POA thread policy value. */
public final class ThreadPolicyValue {

  public static final int _ORB_CTRL_MODEL = 0;
  public static final int _SINGLE_THREAD_MODEL = 1;

  public static final ThreadPolicyValue ORB_CTRL_MODEL = new ThreadPolicyValue(_ORB_CTRL_MODEL);
  public static final ThreadPolicyValue SINGLE_THREAD_MODEL =
      new ThreadPolicyValue(_SINGLE_THREAD_MODEL);

  private final int value;

  private ThreadPolicyValue(int value) {
    this.value = value;
  }

  /** Returns the integer constant value. */
  public int value() {
    return value;
  }
}
