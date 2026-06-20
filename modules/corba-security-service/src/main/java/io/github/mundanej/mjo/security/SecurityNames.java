package io.github.mundanej.mjo.security;

final class SecurityNames {

  private SecurityNames() {}

  static String requireIdentifier(String value, String label, SecurityServiceOptions options) {
    if (value == null || value.isBlank()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_IDENTIFIER, label + " must not be blank");
    }
    if (value.length() > options.maxIdentifierLength()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_IDENTIFIER,
          label + " exceeds " + options.maxIdentifierLength() + " characters");
    }
    return value;
  }
}
