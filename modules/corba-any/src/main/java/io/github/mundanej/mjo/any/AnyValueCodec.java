package io.github.mundanej.mjo.any;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import java.util.Objects;

/**
 * Static CDR payload codec for local Any values.
 *
 * @param <T> payload type
 */
public interface AnyValueCodec<T> {

  /** Returns the TypeCode this codec reads and writes. */
  IdlTypeCode typeCode();

  /** Reads one payload value from CDR. */
  T read(CdrReader reader);

  /** Writes one payload value to CDR. */
  void write(CdrWriter writer, T value);

  /** Reads one local Any value with this codec's TypeCode. */
  default AnyValue<T> readAny(CdrReader reader) {
    return new AnyValue<>(typeCode(), read(reader));
  }

  /** Writes one local Any value after checking its TypeCode. */
  default void writeAny(CdrWriter writer, AnyValue<T> value) {
    Objects.requireNonNull(value, "value");
    if (!typeCode().equals(value.typeCode())) {
      throw new AnyException(
          AnyDiagnosticCodes.TYPE_MISMATCH,
          "Any TypeCode does not match codec TypeCode: "
              + value.typeCode().kind()
              + " vs "
              + typeCode().kind());
    }
    write(writer, value.value());
  }
}
