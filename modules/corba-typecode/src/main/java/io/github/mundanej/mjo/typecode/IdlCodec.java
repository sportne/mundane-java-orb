package io.github.mundanej.mjo.typecode;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;

/**
 * Generated CDR codec surface for one IDL value or operation payload.
 *
 * @param <T> Java value shape handled by the codec
 */
public interface IdlCodec<T> {

  /** Reads a value from CDR input. */
  T read(CdrReader reader);

  /** Writes a value to CDR output. */
  void write(CdrWriter writer, T value);
}
