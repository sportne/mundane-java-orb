package io.github.mundanej.mjo.security;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Stable local policy evaluation reason without raw credential material. */
public record SecurityPolicyEvaluationReason(DiagnosticCode code, String message) {

  /** Maximum reason text length for bounded local diagnostics. */
  public static final int MAX_MESSAGE_LENGTH = 96;

  /** Creates a bounded, deterministic reason. */
  public SecurityPolicyEvaluationReason {
    Objects.requireNonNull(code, "code");
    message = requireMessage(message);
  }

  private static String requireMessage(String value) {
    Objects.requireNonNull(value, "message");
    if (value.isBlank()) {
      throw new IllegalArgumentException("message must not be blank");
    }
    if (value.length() > MAX_MESSAGE_LENGTH) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.POLICY_EVALUATION_LIMIT_EXCEEDED,
          "policy evaluation reason exceeds " + MAX_MESSAGE_LENGTH + " characters");
    }
    return value;
  }
}
