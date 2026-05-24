package org.omg.CORBA.portable;

/** API-only portable input stream compatibility surface. */
public abstract class InputStream {

  /** Reads a boolean value. */
  public abstract boolean read_boolean();

  /** Reads a char value. */
  public abstract char read_char();

  /** Reads an octet value. */
  public abstract byte read_octet();

  /** Reads a short value. */
  public abstract short read_short();

  /** Reads an unsigned short value. */
  public abstract short read_ushort();

  /** Reads a long value. */
  public abstract int read_long();

  /** Reads an unsigned long value. */
  public abstract int read_ulong();

  /** Reads a long long value. */
  public abstract long read_longlong();

  /** Reads a float value. */
  public abstract float read_float();

  /** Reads a double value. */
  public abstract double read_double();

  /** Reads a string value. */
  public abstract String read_string();

  /** Reads an Any value. */
  public abstract org.omg.CORBA.Any read_any();

  /** Reads an object reference. */
  public abstract org.omg.CORBA.Object read_Object();
}
