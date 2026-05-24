package org.omg.CORBA;

/** Dynamic invocation named-value list compatibility surface. */
public abstract class NVList {

  /** Returns the number of values. */
  public abstract int count();

  /** Adds a value with flags. */
  public abstract NamedValue add(int flags);

  /** Adds a named value with flags. */
  public abstract NamedValue add_item(String name, int flags);

  /** Returns the value at an index. */
  public abstract NamedValue item(int index) throws Bounds;

  /** Removes the value at an index. */
  public abstract void remove(int index) throws Bounds;
}
