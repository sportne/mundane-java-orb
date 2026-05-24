package org.omg.CORBA;

/** Dynamic invocation exception list compatibility surface. */
public abstract class ExceptionList {

  /** Returns the number of exception TypeCodes. */
  public abstract int count();

  /** Adds an exception TypeCode. */
  public abstract void add(TypeCode type);

  /** Returns an exception TypeCode by index. */
  public abstract TypeCode item(int index) throws Bounds;

  /** Removes an exception TypeCode by index. */
  public abstract void remove(int index) throws Bounds;
}
