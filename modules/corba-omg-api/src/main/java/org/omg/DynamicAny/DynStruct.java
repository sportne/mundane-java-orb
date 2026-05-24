package org.omg.DynamicAny;

import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;

/** Dynamic struct compatibility surface. */
public interface DynStruct extends DynAny {

  /** Returns member name/value pairs. */
  NameValuePair[] get_members();

  /** Sets member name/value pairs. */
  void set_members(NameValuePair[] value) throws TypeMismatch, InvalidValue;
}
