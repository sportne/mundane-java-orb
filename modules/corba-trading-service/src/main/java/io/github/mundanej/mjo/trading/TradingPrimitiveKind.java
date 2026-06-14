package io.github.mundanej.mjo.trading;

/** Supported primitive property kinds for the local Trading Service subset. */
public enum TradingPrimitiveKind {
  /** UTF-16 Java text value bounded by later offer slices. */
  STRING,

  /** Boolean property value. */
  BOOLEAN,

  /** Signed 64-bit integer property value. */
  SIGNED_LONG,

  /** Finite 64-bit floating-point property value. */
  FLOATING_POINT
}
