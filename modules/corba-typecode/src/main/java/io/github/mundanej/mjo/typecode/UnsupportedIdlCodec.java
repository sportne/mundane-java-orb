package io.github.mundanej.mjo.typecode;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import java.util.Objects;

/**
 * Compile-only codec used before a later roadmap task supplies functional CDR behavior.
 *
 * @param <T> Java value shape reserved for the eventual codec
 */
public final class UnsupportedIdlCodec<T> implements IdlCodec<T> {

  private final String message;

  /** Creates an unsupported codec with a stable failure message. */
  public UnsupportedIdlCodec(String message) {
    this.message = requireNonBlank(message, "message");
  }

  @Override
  public T read(CdrReader reader) {
    Objects.requireNonNull(reader, "reader");
    throw new UnsupportedOperationException(message);
  }

  @Override
  public void write(CdrWriter writer, T value) {
    Objects.requireNonNull(writer, "writer");
    throw new UnsupportedOperationException(message);
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
