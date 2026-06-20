package io.github.mundanej.mjo.security;

import java.util.List;
import java.util.Objects;

/** Immutable result of local Security Service policy evaluation. */
public record SecurityPolicyEvaluationDecision(
    SecurityPolicyEvaluationStatus status, List<SecurityPolicyEvaluationReason> reasons) {

  /** Creates a deterministic immutable decision. */
  public SecurityPolicyEvaluationDecision {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(reasons, "reasons");
    reasons = List.copyOf(reasons);
    if (reasons.stream().anyMatch(Objects::isNull)) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_POLICY_EVALUATION,
          "policy evaluation reasons must not contain null entries");
    }
    if (status == SecurityPolicyEvaluationStatus.ALLOW && !reasons.isEmpty()) {
      throw malformed("allow decisions must not include reasons");
    }
    if (status == SecurityPolicyEvaluationStatus.CHALLENGE
        && (reasons.isEmpty()
            || reasons.stream()
                .anyMatch(
                    reason ->
                        !reason
                            .code()
                            .equals(SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL)))) {
      throw malformed("challenge decisions require only missing-credential reasons");
    }
    if (status == SecurityPolicyEvaluationStatus.DENY && reasons.isEmpty()) {
      throw malformed("deny decisions must include at least one reason");
    }
  }

  /** Returns a successful allow decision. */
  public static SecurityPolicyEvaluationDecision allow() {
    return new SecurityPolicyEvaluationDecision(SecurityPolicyEvaluationStatus.ALLOW, List.of());
  }

  private static SecurityServiceException malformed(String message) {
    return new SecurityServiceException(
        SecurityServiceDiagnosticCodes.MALFORMED_POLICY_EVALUATION, message);
  }
}
