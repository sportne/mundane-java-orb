package io.github.mundanej.mjo.security;

/** Supported CSIv2 mechanism identifiers for the local metadata subset. */
public final class SecurityCsiv2MechanismIds {

  /** Project-owned mechanism identity for the supported local CSIv2 subset. */
  public static final String SUPPORTED_LOCAL = "MJO-SEC-CSIV2-LOCAL";

  /** Maximum mechanism identity length accepted by this metadata slice. */
  public static final int MAX_MECHANISM_ID_LENGTH = 64;

  private SecurityCsiv2MechanismIds() {}

  /** Returns the validated supported mechanism identity. */
  public static String requireSupported(String value) {
    if (value == null || value.isBlank()) {
      throw malformed("CSIv2 mechanism identity must not be blank");
    }
    if (value.length() > MAX_MECHANISM_ID_LENGTH) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.CSIV2_METADATA_LIMIT_EXCEEDED,
          "CSIv2 mechanism identity exceeds " + MAX_MECHANISM_ID_LENGTH + " characters");
    }
    if (!SUPPORTED_LOCAL.equals(value)) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.UNSUPPORTED_CSIV2_MECHANISM,
          "unsupported CSIv2 mechanism identity");
    }
    return value;
  }

  private static SecurityServiceException malformed(String message) {
    return new SecurityServiceException(
        SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA, message);
  }
}
