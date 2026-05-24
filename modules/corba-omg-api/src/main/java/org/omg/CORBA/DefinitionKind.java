package org.omg.CORBA;

/** Enum-like Interface Repository definition kind. */
public final class DefinitionKind {

  public static final int _dk_none = 0;
  public static final int _dk_all = 1;
  public static final int _dk_Module = 2;
  public static final int _dk_Interface = 3;
  public static final int _dk_Operation = 4;
  public static final int _dk_Attribute = 5;
  public static final int _dk_Constant = 6;
  public static final int _dk_Exception = 7;
  public static final int _dk_Struct = 8;
  public static final int _dk_Union = 9;
  public static final int _dk_Enum = 10;
  public static final int _dk_Alias = 11;
  public static final int _dk_Primitive = 12;
  public static final int _dk_String = 13;
  public static final int _dk_Sequence = 14;
  public static final int _dk_Array = 15;

  public static final DefinitionKind dk_none = new DefinitionKind(_dk_none);
  public static final DefinitionKind dk_all = new DefinitionKind(_dk_all);

  private final int value;

  private DefinitionKind(int value) {
    this.value = value;
  }

  /** Returns the integer constant value. */
  public int value() {
    return value;
  }
}
