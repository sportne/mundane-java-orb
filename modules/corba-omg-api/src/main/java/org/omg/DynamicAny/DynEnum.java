package org.omg.DynamicAny;

import org.omg.DynamicAny.DynAnyPackage.InvalidValue;

/** Dynamic enum compatibility surface. */
public interface DynEnum extends DynAny {

  /** Returns the enum value as a string. */
  String get_as_string();

  /** Sets the enum value from a string. */
  void set_as_string(String value) throws InvalidValue;

  /** Returns the enum value as an unsigned long. */
  int get_as_ulong();

  /** Sets the enum value from an unsigned long. */
  void set_as_ulong(int value) throws InvalidValue;
}
