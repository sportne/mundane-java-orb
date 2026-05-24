package org.omg.DynamicAny;

import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;

/** Dynamic union compatibility surface. */
public interface DynUnion extends DynAny {

  /** Returns the discriminator value. */
  DynAny get_discriminator();

  /** Sets the discriminator value. */
  void set_discriminator(DynAny discriminator) throws TypeMismatch;

  /** Returns whether no active member is selected. */
  boolean has_no_active_member();

  /** Returns the active member. */
  DynAny member() throws InvalidValue;
}
