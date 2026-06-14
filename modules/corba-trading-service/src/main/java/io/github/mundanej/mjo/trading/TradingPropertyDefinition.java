package io.github.mundanej.mjo.trading;

/** Immutable service type property definition for the supported local subset. */
public record TradingPropertyDefinition(String name, TradingPrimitiveKind kind, boolean required) {

  /** Creates a validated property definition. */
  public TradingPropertyDefinition {
    name = TradingNames.requireName(name, "property name", TradingServiceOptions.modelLimits());
    kind = requireKind(kind);
  }

  /** Creates a required string property definition. */
  public static TradingPropertyDefinition requiredString(String name) {
    return new TradingPropertyDefinition(name, TradingPrimitiveKind.STRING, true);
  }

  /** Creates a required boolean property definition. */
  public static TradingPropertyDefinition requiredBoolean(String name) {
    return new TradingPropertyDefinition(name, TradingPrimitiveKind.BOOLEAN, true);
  }

  /** Creates a required signed 64-bit integer property definition. */
  public static TradingPropertyDefinition requiredSignedLong(String name) {
    return new TradingPropertyDefinition(name, TradingPrimitiveKind.SIGNED_LONG, true);
  }

  /** Creates a required finite floating-point property definition. */
  public static TradingPropertyDefinition requiredFloatingPoint(String name) {
    return new TradingPropertyDefinition(name, TradingPrimitiveKind.FLOATING_POINT, true);
  }

  private static TradingPrimitiveKind requireKind(TradingPrimitiveKind kind) {
    if (kind == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_TYPE, "property kind must not be null");
    }
    return kind;
  }
}
