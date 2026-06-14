package io.github.mundanej.mjo.trading;

/** Caller-provided local Trading Service offer repository limits. */
public record TradingOfferRepositoryOptions(
    int maxOffers, int maxPropertiesPerOffer, int maxOfferIdLength, int maxStringValueLength) {

  /** Default maximum number of offers owned by one local repository. */
  public static final int DEFAULT_MAX_OFFERS = 1_024;

  /** Default maximum number of property values carried by one offer. */
  public static final int DEFAULT_MAX_PROPERTIES_PER_OFFER = 64;

  /** Default maximum length for offer IDs. */
  public static final int DEFAULT_MAX_OFFER_ID_LENGTH = 128;

  /** Default maximum length for string property values. */
  public static final int DEFAULT_MAX_STRING_VALUE_LENGTH = 4_096;

  /** Creates validated Trading Service offer repository options. */
  public TradingOfferRepositoryOptions {
    requireLimit(maxOffers, "maxOffers");
    requireLimit(maxPropertiesPerOffer, "maxPropertiesPerOffer");
    requireLimit(maxOfferIdLength, "maxOfferIdLength");
    requireLimit(maxStringValueLength, "maxStringValueLength");
  }

  /** Returns default bounded local Trading Service offer repository options. */
  public static TradingOfferRepositoryOptions defaults() {
    return new TradingOfferRepositoryOptions(
        DEFAULT_MAX_OFFERS,
        DEFAULT_MAX_PROPERTIES_PER_OFFER,
        DEFAULT_MAX_OFFER_ID_LENGTH,
        DEFAULT_MAX_STRING_VALUE_LENGTH);
  }

  private static void requireLimit(int value, String name) {
    if (value < 1 || value > TradingServiceOptions.MAX_SUPPORTED_LIMIT) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.INVALID_LIMIT,
          name + " must be between 1 and " + TradingServiceOptions.MAX_SUPPORTED_LIMIT);
    }
  }
}
