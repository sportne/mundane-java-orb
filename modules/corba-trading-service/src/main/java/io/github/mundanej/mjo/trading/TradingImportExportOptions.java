package io.github.mundanej.mjo.trading;

/** Caller-provided local Trading Service import/export boundary limits. */
public record TradingImportExportOptions(
    int maxLinks, int maxLinkNameLength, int maxPeerTraderNameLength) {

  /** Default maximum number of import/export links owned by one boundary. */
  public static final int DEFAULT_MAX_LINKS = 64;

  /** Default maximum length for import/export link names. */
  public static final int DEFAULT_MAX_LINK_NAME_LENGTH = 128;

  /** Default maximum length for peer trader names. */
  public static final int DEFAULT_MAX_PEER_TRADER_NAME_LENGTH = 128;

  /** Creates validated Trading Service import/export boundary options. */
  public TradingImportExportOptions {
    requireLimit(maxLinks, "maxLinks");
    requireLimit(maxLinkNameLength, "maxLinkNameLength");
    requireLimit(maxPeerTraderNameLength, "maxPeerTraderNameLength");
  }

  /** Returns default bounded local Trading Service import/export boundary options. */
  public static TradingImportExportOptions defaults() {
    return new TradingImportExportOptions(
        DEFAULT_MAX_LINKS, DEFAULT_MAX_LINK_NAME_LENGTH, DEFAULT_MAX_PEER_TRADER_NAME_LENGTH);
  }

  private static void requireLimit(int value, String name) {
    if (value < 1 || value > TradingServiceOptions.MAX_SUPPORTED_LIMIT) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.INVALID_LIMIT,
          name + " must be between 1 and " + TradingServiceOptions.MAX_SUPPORTED_LIMIT);
    }
  }
}
