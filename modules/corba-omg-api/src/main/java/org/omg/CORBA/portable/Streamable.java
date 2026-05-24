package org.omg.CORBA.portable;

/** Holder contract for generated IDL holder classes. */
public interface Streamable {

  /** Reads the holder value from an input stream. */
  void _read(InputStream input);

  /** Writes the holder value to an output stream. */
  void _write(OutputStream output);

  /** Returns the holder TypeCode. */
  org.omg.CORBA.TypeCode _type();
}
