package org.omg.DynamicAny;

import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;

/** DynamicAny compatibility surface. */
public interface DynAny {

  /** Returns this dynamic value as an Any. */
  org.omg.CORBA.Any to_any();

  /** Assigns another dynamic value. */
  void assign(DynAny dynAny) throws TypeMismatch;

  /** Initializes this dynamic value from an Any. */
  void from_any(org.omg.CORBA.Any value) throws TypeMismatch, InvalidValue;

  /** Returns this value's TypeCode. */
  org.omg.CORBA.TypeCode type();

  /** Destroys this dynamic value. */
  void destroy();

  /** Creates a copy of this dynamic value. */
  DynAny copy();

  /** Advances to the next component. */
  boolean next();

  /** Rewinds to the first component. */
  void rewind();

  /** Seeks to a component index. */
  boolean seek(int index);

  /** Returns the current component. */
  DynAny current_component() throws TypeMismatch;
}
