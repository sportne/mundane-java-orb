package org.omg.CORBA;

/** Enum-like policy override mode. */
public final class SetOverrideType {

  public static final int _SET_OVERRIDE = 0;
  public static final int _ADD_OVERRIDE = 1;

  public static final SetOverrideType SET_OVERRIDE = new SetOverrideType(_SET_OVERRIDE);
  public static final SetOverrideType ADD_OVERRIDE = new SetOverrideType(_ADD_OVERRIDE);

  private final int value;

  private SetOverrideType(int value) {
    this.value = value;
  }

  /** Returns the integer constant value. */
  public int value() {
    return value;
  }

  /** Returns the enum-like instance for a known override value. */
  public static SetOverrideType from_int(int value) {
    return switch (value) {
      case _SET_OVERRIDE -> SET_OVERRIDE;
      case _ADD_OVERRIDE -> ADD_OVERRIDE;
      default -> throw new BAD_PARAM("Unknown SetOverrideType value: " + value);
    };
  }
}
