package io.github.mundanej.mjo.trading;

/** Caller-provided local Trading Service type repository limits. */
public record TradingServiceOptions(int maxTypes, int maxPropertiesPerType, int maxNameLength) {

  /** Default maximum number of service types owned by one local repository. */
  public static final int DEFAULT_MAX_TYPES = 256;

  /** Default maximum number of property definitions on one service type. */
  public static final int DEFAULT_MAX_PROPERTIES_PER_TYPE = 64;

  /** Default maximum length for service type and property names. */
  public static final int DEFAULT_MAX_NAME_LENGTH = 128;

  /** Maximum supported bound for any configured Trading Service limit. */
  public static final int MAX_SUPPORTED_LIMIT = 65_535;

  /** Creates validated Trading Service options. */
  public TradingServiceOptions {
    requireLimit(maxTypes, "maxTypes");
    requireLimit(maxPropertiesPerType, "maxPropertiesPerType");
    requireLimit(maxNameLength, "maxNameLength");
  }

  /** Returns default bounded local Trading Service options. */
  public static TradingServiceOptions defaults() {
    return new TradingServiceOptions(
        DEFAULT_MAX_TYPES, DEFAULT_MAX_PROPERTIES_PER_TYPE, DEFAULT_MAX_NAME_LENGTH);
  }

  static TradingServiceOptions modelLimits() {
    return new TradingServiceOptions(MAX_SUPPORTED_LIMIT, MAX_SUPPORTED_LIMIT, MAX_SUPPORTED_LIMIT);
  }

  private static void requireLimit(int value, String name) {
    if (value < 1 || value > MAX_SUPPORTED_LIMIT) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.INVALID_LIMIT,
          name + " must be between 1 and " + MAX_SUPPORTED_LIMIT);
    }
  }
}
