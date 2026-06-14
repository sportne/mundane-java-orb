package io.github.mundanej.mjo.trading;

final class TradingNames {

  private TradingNames() {}

  static String requireName(String value, String label, TradingServiceOptions options) {
    if (value == null || value.isBlank()) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_NAME, label + " must not be blank");
    }
    if (value.length() > options.maxNameLength()) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_NAME,
          label + " exceeds " + options.maxNameLength() + " characters");
    }
    return value;
  }
}
