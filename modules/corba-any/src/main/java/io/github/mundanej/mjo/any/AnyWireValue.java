package io.github.mundanej.mjo.any;

import io.github.mundanej.mjo.typecode.WireTypeCode;
import java.util.Objects;

/** Wire Any value containing a wire TypeCode and its CDR value body. */
public record AnyWireValue(WireTypeCode typeCode, Object value) {

  /** Creates a validated wire Any value. */
  public AnyWireValue {
    Objects.requireNonNull(typeCode, "typeCode");
    Objects.requireNonNull(value, "value");
  }
}
