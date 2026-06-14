package io.github.mundanej.mjo.trading;

/** Immutable import/export boundary metadata for the supported local Trading Service subset. */
public record TradingImportExportLink(
    String name, TradingImportExportDirection direction, String peerTraderName) {

  /** Creates validated link metadata using maximum supported standalone model limits. */
  public TradingImportExportLink {
    name =
        TradingNames.requireName(
            name, "import/export link name", TradingServiceOptions.modelLimits());
    if (direction == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_LINK, "link direction must not be null");
    }
    peerTraderName =
        TradingNames.requireName(
            peerTraderName, "import/export peer trader name", TradingServiceOptions.modelLimits());
  }

  static TradingImportExportLink validateForBoundary(
      TradingImportExportLink link, TradingImportExportOptions options) {
    if (link == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_LINK, "import/export link must not be null");
    }
    String linkName =
        TradingNames.requireBoundedText(
            link.name(), "import/export link name", options.maxLinkNameLength());
    String peerTrader =
        TradingNames.requireBoundedText(
            link.peerTraderName(),
            "import/export peer trader name",
            options.maxPeerTraderNameLength());
    return new TradingImportExportLink(linkName, link.direction(), peerTrader);
  }
}
