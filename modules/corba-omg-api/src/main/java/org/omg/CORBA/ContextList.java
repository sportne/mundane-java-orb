package org.omg.CORBA;

/** Dynamic invocation context list compatibility surface. */
public abstract class ContextList {

  /** Returns the number of context strings. */
  public abstract int count();

  /** Adds a context string. */
  public abstract void add(String context);

  /** Returns a context string by index. */
  public abstract String item(int index) throws Bounds;

  /** Removes a context string by index. */
  public abstract void remove(int index) throws Bounds;
}
