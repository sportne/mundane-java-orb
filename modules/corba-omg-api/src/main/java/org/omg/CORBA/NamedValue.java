package org.omg.CORBA;

/** Named dynamic invocation value compatibility surface. */
public abstract class NamedValue {

  /** Returns the argument name. */
  public abstract String name();

  /** Returns the argument value. */
  public abstract Any value();

  /** Returns the argument flags. */
  public abstract int flags();
}
