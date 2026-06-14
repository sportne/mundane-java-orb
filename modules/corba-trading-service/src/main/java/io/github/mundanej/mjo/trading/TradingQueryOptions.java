package io.github.mundanej.mjo.trading;

/** Caller-provided local Trading Service query limits. */
public record TradingQueryOptions(int maxResults, int maxCost) {

  /** Default maximum number of offers a local query may return. */
  public static final int DEFAULT_MAX_RESULTS = 256;

  /** Default maximum number of type-scoped offers a local query may inspect. */
  public static final int DEFAULT_MAX_COST = 1_024;

  /** Creates validated local query options. */
  public TradingQueryOptions {
    requireLimit(maxResults, "maxResults");
    requireLimit(maxCost, "maxCost");
  }

  /** Returns default bounded local Trading Service query options. */
  public static TradingQueryOptions defaults() {
    return new TradingQueryOptions(DEFAULT_MAX_RESULTS, DEFAULT_MAX_COST);
  }

  private static void requireLimit(int value, String name) {
    if (value < 1 || value > TradingServiceOptions.MAX_SUPPORTED_LIMIT) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.INVALID_LIMIT,
          name + " must be between 1 and " + TradingServiceOptions.MAX_SUPPORTED_LIMIT);
    }
  }
}
