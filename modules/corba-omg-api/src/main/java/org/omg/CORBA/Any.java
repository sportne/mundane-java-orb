package org.omg.CORBA;

/** API-only CORBA Any compatibility surface. */
public abstract class Any {

  /** Returns the TypeCode for this Any value. */
  public abstract TypeCode type();

  /** Sets the TypeCode for this Any value. */
  public abstract void type(TypeCode type);

  /** Returns whether another Any is equivalent to this value. */
  public abstract boolean equal(Any other);

  /** Inserts a long value. */
  public abstract void insert_long(int value);

  /** Extracts a long value. */
  public abstract int extract_long();

  /** Inserts a string value. */
  public abstract void insert_string(String value);

  /** Extracts a string value. */
  public abstract String extract_string();

  /** Inserts an object reference. */
  public abstract void insert_Object(Object value);

  /** Extracts an object reference. */
  public abstract Object extract_Object();
}
