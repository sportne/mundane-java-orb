package org.omg.PortableServer;

/** Enum-like POA ID assignment policy value. */
public final class IdAssignmentPolicyValue {

  public static final int _USER_ID = 0;
  public static final int _SYSTEM_ID = 1;

  public static final IdAssignmentPolicyValue USER_ID = new IdAssignmentPolicyValue(_USER_ID);
  public static final IdAssignmentPolicyValue SYSTEM_ID = new IdAssignmentPolicyValue(_SYSTEM_ID);

  private final int value;

  private IdAssignmentPolicyValue(int value) {
    this.value = value;
  }

  /** Returns the integer constant value. */
  public int value() {
    return value;
  }
}
