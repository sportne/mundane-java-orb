package io.github.mundanej.mjo.security;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.Objects;

/** Stable redacted failure disclosure for local Security Service diagnostics. */
public record SecurityFailureDisclosure(DiagnosticCode code, String reason) {

  /** Maximum stable failure reason length. */
  public static final int MAX_REASON_LENGTH = 96;

  /** Creates a stable failure disclosure. */
  public SecurityFailureDisclosure {
    Objects.requireNonNull(code, "code");
    reason = requireReason(reason);
  }

  /** Maps an evaluator reason to a redacted stable disclosure. */
  public static SecurityFailureDisclosure from(SecurityPolicyEvaluationReason reason) {
    Objects.requireNonNull(reason, "reason");
    return new SecurityFailureDisclosure(reason.code(), stableReason(reason.code()));
  }

  /** Maps an exception code to a redacted stable disclosure without copying its message. */
  public static SecurityFailureDisclosure from(SecurityServiceException exception) {
    Objects.requireNonNull(exception, "exception");
    return new SecurityFailureDisclosure(exception.code(), stableReason(exception.code()));
  }

  private static String requireReason(String value) {
    Objects.requireNonNull(value, "reason");
    if (value.isBlank()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_AUDIT_EVENT,
          "failure disclosure reason must not be blank");
    }
    String redacted =
        isStableReason(value) ? value : SecurityAuditRedaction.redact("failure", value);
    if (redacted.length() > MAX_REASON_LENGTH) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.AUDIT_EVENT_LIMIT_EXCEEDED,
          "failure disclosure reason exceeds " + MAX_REASON_LENGTH + " characters");
    }
    return redacted;
  }

  private static boolean isStableReason(String value) {
    return value.equals("credential required")
        || value.equals("credential expired")
        || value.equals("credential untrusted")
        || value.equals("delegation unsupported")
        || value.equals("transport protection insufficient")
        || value.equals("csiv2 metadata malformed")
        || value.equals("policy conflict")
        || value.equals("policy evaluation malformed")
        || value.equals("security failure");
  }

  private static String stableReason(DiagnosticCode code) {
    if (code.equals(SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL)) {
      return "credential required";
    }
    if (code.equals(SecurityServiceDiagnosticCodes.CREDENTIAL_EXPIRED)) {
      return "credential expired";
    }
    if (code.equals(SecurityServiceDiagnosticCodes.CREDENTIAL_UNTRUSTED)) {
      return "credential untrusted";
    }
    if (code.equals(SecurityServiceDiagnosticCodes.UNSUPPORTED_DELEGATION)) {
      return "delegation unsupported";
    }
    if (code.equals(SecurityServiceDiagnosticCodes.INSUFFICIENT_TRANSPORT_PROTECTION)) {
      return "transport protection insufficient";
    }
    if (code.equals(SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA)) {
      return "csiv2 metadata malformed";
    }
    if (code.equals(SecurityServiceDiagnosticCodes.CONFLICTING_POLICY)) {
      return "policy conflict";
    }
    if (code.equals(SecurityServiceDiagnosticCodes.MALFORMED_POLICY_EVALUATION)) {
      return "policy evaluation malformed";
    }
    return "security failure";
  }
}
