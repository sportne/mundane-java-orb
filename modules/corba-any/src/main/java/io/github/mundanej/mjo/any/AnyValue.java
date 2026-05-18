package io.github.mundanej.mjo.any;

import io.github.mundanej.mjo.typecode.IdlTypeCode;
import java.util.Objects;

/**
 * Local Any value paired with descriptor-backed TypeCode metadata.
 *
 * @param typeCode static local TypeCode
 * @param value payload value
 * @param <T> payload type
 */
public record AnyValue<T>(IdlTypeCode typeCode, T value) {

  /** Creates a non-null Any value. */
  public AnyValue {
    Objects.requireNonNull(typeCode, "typeCode");
    Objects.requireNonNull(value, "value");
  }
}
