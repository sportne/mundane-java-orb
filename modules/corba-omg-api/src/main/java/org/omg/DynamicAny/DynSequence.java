package org.omg.DynamicAny;

import org.omg.DynamicAny.DynAnyPackage.InvalidValue;

/** Dynamic sequence compatibility surface. */
public interface DynSequence extends DynAny {

  /** Returns sequence length. */
  int get_length();

  /** Sets sequence length. */
  void set_length(int length) throws InvalidValue;
}
