package org.omg.CORBA.portable;

/** API-only portable output stream compatibility surface. */
public abstract class OutputStream {

  /** Writes a boolean value. */
  public abstract void write_boolean(boolean value);

  /** Writes a char value. */
  public abstract void write_char(char value);

  /** Writes an octet value. */
  public abstract void write_octet(byte value);

  /** Writes a short value. */
  public abstract void write_short(short value);

  /** Writes an unsigned short value. */
  public abstract void write_ushort(short value);

  /** Writes a long value. */
  public abstract void write_long(int value);

  /** Writes an unsigned long value. */
  public abstract void write_ulong(int value);

  /** Writes a long long value. */
  public abstract void write_longlong(long value);

  /** Writes a float value. */
  public abstract void write_float(float value);

  /** Writes a double value. */
  public abstract void write_double(double value);

  /** Writes a string value. */
  public abstract void write_string(String value);

  /** Writes an Any value. */
  public abstract void write_any(org.omg.CORBA.Any value);

  /** Writes an object reference. */
  public abstract void write_Object(org.omg.CORBA.Object value);

  /** Creates an input stream over the written data. */
  public abstract InputStream create_input_stream();
}
