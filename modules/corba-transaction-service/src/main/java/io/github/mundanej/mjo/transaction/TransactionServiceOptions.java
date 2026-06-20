package io.github.mundanej.mjo.transaction;

/** Caller-provided local Transaction Service coordinator limits. */
public record TransactionServiceOptions(
    int maxTransactions, int maxResourcesPerTransaction, int maxIdentifierLength) {

  /** Default maximum number of local transactions owned by one coordinator. */
  public static final int DEFAULT_MAX_TRANSACTIONS = 256;

  /** Default maximum number of resources enlisted with one local transaction. */
  public static final int DEFAULT_MAX_RESOURCES_PER_TRANSACTION = 64;

  /** Default maximum length for local transaction and resource identifiers. */
  public static final int DEFAULT_MAX_IDENTIFIER_LENGTH = 128;

  /** Maximum supported bound for any configured Transaction Service limit. */
  public static final int MAX_SUPPORTED_LIMIT = 65_535;

  /** Creates validated Transaction Service options. */
  public TransactionServiceOptions {
    requireLimit(maxTransactions, "maxTransactions");
    requireLimit(maxResourcesPerTransaction, "maxResourcesPerTransaction");
    requireLimit(maxIdentifierLength, "maxIdentifierLength");
  }

  /** Returns default bounded local Transaction Service options. */
  public static TransactionServiceOptions defaults() {
    return new TransactionServiceOptions(
        DEFAULT_MAX_TRANSACTIONS,
        DEFAULT_MAX_RESOURCES_PER_TRANSACTION,
        DEFAULT_MAX_IDENTIFIER_LENGTH);
  }

  static TransactionServiceOptions modelLimits() {
    return new TransactionServiceOptions(
        MAX_SUPPORTED_LIMIT, MAX_SUPPORTED_LIMIT, MAX_SUPPORTED_LIMIT);
  }

  private static void requireLimit(int value, String name) {
    if (value < 1 || value > MAX_SUPPORTED_LIMIT) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.INVALID_LIMIT,
          name + " must be between 1 and " + MAX_SUPPORTED_LIMIT);
    }
  }
}
