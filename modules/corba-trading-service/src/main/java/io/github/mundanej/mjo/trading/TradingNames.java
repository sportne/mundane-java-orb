package io.github.mundanej.mjo.trading;

final class TradingNames {

  private TradingNames() {}

  static String requireName(String value, String label, TradingServiceOptions options) {
    return requireBoundedText(value, label, options.maxNameLength());
  }

  static String requireBoundedText(String value, String label, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_NAME, label + " must not be blank");
    }
    if (value.length() > maxLength) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_NAME,
          label + " exceeds " + maxLength + " characters");
    }
    return value;
  }
}
