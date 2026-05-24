package org.omg.CosNaming;

/** CosNaming binding iterator compatibility surface. */
public interface BindingIterator extends org.omg.CORBA.Object {

  /** Returns the next binding. */
  boolean next_one(BindingHolder binding);

  /** Returns up to the requested number of bindings. */
  boolean next_n(int howMany, BindingListHolder bindings);

  /** Destroys the iterator. */
  void destroy();
}
