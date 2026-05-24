package org.omg.CORBA;

/** Dynamic invocation context compatibility surface. */
public abstract class Context {

  /** Returns the context name. */
  public abstract String context_name();

  /** Returns the parent context. */
  public abstract Context parent();

  /** Creates a child context. */
  public abstract Context create_child(String childContextName);
}
