package org.omg.CORBA;

/** Dynamic invocation environment compatibility surface. */
public abstract class Environment {

  /** Returns the current exception. */
  public abstract java.lang.Exception exception();

  /** Sets the current exception. */
  public abstract void exception(java.lang.Exception exception);

  /** Clears the current exception. */
  public abstract void clear();
}
