package io.github.mundanej.mjo.security;

import java.time.Instant;
import java.util.Objects;

final class SecurityLifetimes {

  private SecurityLifetimes() {}

  static void requireValidLifetime(Instant issuedAt, Instant expiresAt) {
    if (issuedAt == null || expiresAt == null) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_LIFETIME,
          "credential lifetime metadata must not be null");
    }
    if (!expiresAt.isAfter(issuedAt)) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_LIFETIME,
          "credential expiresAt must be after issuedAt");
    }
  }

  static void requireValidAt(SecurityTrustEvaluationInput input, Instant now) {
    Objects.requireNonNull(input, "input");
    Objects.requireNonNull(now, "now");
    if (now.isBefore(input.issuedAt()) || !now.isBefore(input.expiresAt())) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.CREDENTIAL_EXPIRED,
          "credential is not valid at evaluation instant: " + input.credentialId().value());
    }
  }
}
