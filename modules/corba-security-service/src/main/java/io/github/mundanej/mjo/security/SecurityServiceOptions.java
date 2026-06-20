package io.github.mundanej.mjo.security;

/** Caller-provided local Security Service credential/trust limits. */
public record SecurityServiceOptions(
    int maxCredentials, int maxTrustAnchors, int maxIdentifierLength) {

  /** Default maximum number of local credentials tracked by one model. */
  public static final int DEFAULT_MAX_CREDENTIALS = 256;

  /** Default maximum number of local trust anchors tracked by one model. */
  public static final int DEFAULT_MAX_TRUST_ANCHORS = 64;

  /** Default maximum length for principal, credential, and trust-anchor identifiers. */
  public static final int DEFAULT_MAX_IDENTIFIER_LENGTH = 128;

  /** Maximum supported bound for any configured Security Service limit. */
  public static final int MAX_SUPPORTED_LIMIT = 65_535;

  /** Creates validated Security Service options. */
  public SecurityServiceOptions {
    requireLimit(maxCredentials, "maxCredentials");
    requireLimit(maxTrustAnchors, "maxTrustAnchors");
    requireLimit(maxIdentifierLength, "maxIdentifierLength");
  }

  /** Returns default bounded local Security Service options. */
  public static SecurityServiceOptions defaults() {
    return new SecurityServiceOptions(
        DEFAULT_MAX_CREDENTIALS, DEFAULT_MAX_TRUST_ANCHORS, DEFAULT_MAX_IDENTIFIER_LENGTH);
  }

  static SecurityServiceOptions modelLimits() {
    return new SecurityServiceOptions(
        MAX_SUPPORTED_LIMIT, MAX_SUPPORTED_LIMIT, MAX_SUPPORTED_LIMIT);
  }

  private static void requireLimit(int value, String name) {
    if (value < 1 || value > MAX_SUPPORTED_LIMIT) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.INVALID_LIMIT,
          name + " must be between 1 and " + MAX_SUPPORTED_LIMIT);
    }
  }
}
