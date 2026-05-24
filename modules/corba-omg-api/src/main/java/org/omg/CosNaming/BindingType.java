package org.omg.CosNaming;

/** Enum-like CosNaming binding type. */
public final class BindingType {

  public static final int _nobject = 0;
  public static final int _ncontext = 1;

  public static final BindingType nobject = new BindingType(_nobject);
  public static final BindingType ncontext = new BindingType(_ncontext);

  private final int value;

  private BindingType(int value) {
    this.value = value;
  }

  /** Returns the integer constant value. */
  public int value() {
    return value;
  }

  /** Returns the enum-like instance for a known binding type. */
  public static BindingType from_int(int value) {
    return switch (value) {
      case _nobject -> nobject;
      case _ncontext -> ncontext;
      default -> throw new org.omg.CORBA.BAD_PARAM("Unknown BindingType value: " + value);
    };
  }
}
