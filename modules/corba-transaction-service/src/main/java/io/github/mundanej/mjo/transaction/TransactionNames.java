package io.github.mundanej.mjo.transaction;

final class TransactionNames {

  private TransactionNames() {}

  static String requireIdentifier(String value, String label, TransactionServiceOptions options) {
    if (value == null || value.isBlank()) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.MALFORMED_IDENTIFIER, label + " must not be blank");
    }
    if (value.length() > options.maxIdentifierLength()) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.MALFORMED_IDENTIFIER,
          label + " exceeds " + options.maxIdentifierLength() + " characters");
    }
    return value;
  }
}
