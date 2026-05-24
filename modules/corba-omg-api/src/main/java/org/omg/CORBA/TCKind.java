package org.omg.CORBA;

/** Enum-like TypeCode kind constants. */
public final class TCKind {

  public static final int _tk_null = 0;
  public static final int _tk_void = 1;
  public static final int _tk_short = 2;
  public static final int _tk_long = 3;
  public static final int _tk_ushort = 4;
  public static final int _tk_ulong = 5;
  public static final int _tk_float = 6;
  public static final int _tk_double = 7;
  public static final int _tk_boolean = 8;
  public static final int _tk_char = 9;
  public static final int _tk_octet = 10;
  public static final int _tk_any = 11;
  public static final int _tk_TypeCode = 12;
  public static final int _tk_Principal = 13;
  public static final int _tk_objref = 14;
  public static final int _tk_struct = 15;
  public static final int _tk_union = 16;
  public static final int _tk_enum = 17;
  public static final int _tk_string = 18;
  public static final int _tk_sequence = 19;
  public static final int _tk_array = 20;
  public static final int _tk_alias = 21;
  public static final int _tk_except = 22;
  public static final int _tk_longlong = 23;
  public static final int _tk_ulonglong = 24;
  public static final int _tk_longdouble = 25;
  public static final int _tk_wchar = 26;
  public static final int _tk_wstring = 27;
  public static final int _tk_fixed = 28;
  public static final int _tk_value = 29;
  public static final int _tk_value_box = 30;
  public static final int _tk_native = 31;
  public static final int _tk_abstract_interface = 32;

  public static final TCKind tk_null = new TCKind(_tk_null);
  public static final TCKind tk_void = new TCKind(_tk_void);
  public static final TCKind tk_short = new TCKind(_tk_short);
  public static final TCKind tk_long = new TCKind(_tk_long);
  public static final TCKind tk_string = new TCKind(_tk_string);
  public static final TCKind tk_objref = new TCKind(_tk_objref);
  public static final TCKind tk_struct = new TCKind(_tk_struct);
  public static final TCKind tk_union = new TCKind(_tk_union);
  public static final TCKind tk_enum = new TCKind(_tk_enum);
  public static final TCKind tk_sequence = new TCKind(_tk_sequence);
  public static final TCKind tk_array = new TCKind(_tk_array);
  public static final TCKind tk_alias = new TCKind(_tk_alias);
  public static final TCKind tk_except = new TCKind(_tk_except);

  private final int value;

  private TCKind(int value) {
    this.value = value;
  }

  /** Returns the integer constant value. */
  public int value() {
    return value;
  }

  /** Returns the enum-like instance for a known TypeCode kind. */
  public static TCKind from_int(int value) {
    return switch (value) {
      case _tk_null -> tk_null;
      case _tk_void -> tk_void;
      case _tk_short -> tk_short;
      case _tk_long -> tk_long;
      case _tk_string -> tk_string;
      case _tk_objref -> tk_objref;
      case _tk_struct -> tk_struct;
      case _tk_union -> tk_union;
      case _tk_enum -> tk_enum;
      case _tk_sequence -> tk_sequence;
      case _tk_array -> tk_array;
      case _tk_alias -> tk_alias;
      case _tk_except -> tk_except;
      default -> throw new BAD_PARAM("Unknown TCKind value: " + value);
    };
  }
}
