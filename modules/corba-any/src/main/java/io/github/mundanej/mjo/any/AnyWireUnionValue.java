package io.github.mundanej.mjo.any;

import java.util.Objects;

/** Wire Any union branch value selected by a numeric discriminator label. */
public record AnyWireUnionValue(long label, AnyWireValue value) {

  /** Creates a validated union value. */
  public AnyWireUnionValue {
    Objects.requireNonNull(value, "value");
  }
}
