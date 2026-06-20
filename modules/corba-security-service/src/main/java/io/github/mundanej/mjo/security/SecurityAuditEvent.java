package io.github.mundanej.mjo.security;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Immutable redacted audit event for local Security Service decisions. */
public record SecurityAuditEvent(
    SecurityAuditEventType type,
    Instant occurredAt,
    SecurityPolicyEvaluationStatus status,
    List<SecurityFailureDisclosure> failures,
    List<SecurityAuditField> fields) {

  /** Creates a bounded immutable redacted audit event. */
  public SecurityAuditEvent {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(occurredAt, "occurredAt");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(failures, "failures");
    Objects.requireNonNull(fields, "fields");
    if (failures.stream().anyMatch(Objects::isNull) || fields.stream().anyMatch(Objects::isNull)) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_AUDIT_EVENT,
          "audit event entries must not contain null values");
    }
    failures =
        List.copyOf(
            failures.stream()
                .sorted(Comparator.comparing(disclosure -> disclosure.code().value()))
                .toList());
    fields =
        List.copyOf(
            fields.stream()
                .sorted(
                    Comparator.comparing(SecurityAuditField::key)
                        .thenComparing(SecurityAuditField::value))
                .toList());
    if (failures.size() > SecurityAuditOptions.ABSOLUTE_MAX_FAILURES
        || fields.size() > SecurityAuditOptions.ABSOLUTE_MAX_FIELDS) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.AUDIT_EVENT_LIMIT_EXCEEDED,
          "audit event exceeds absolute entry limits");
    }
    if (status == SecurityPolicyEvaluationStatus.ALLOW && !failures.isEmpty()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_AUDIT_EVENT,
          "allow audit events must not include failure disclosures");
    }
    if (status != SecurityPolicyEvaluationStatus.ALLOW && failures.isEmpty()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_AUDIT_EVENT,
          "non-allow audit events must include failure disclosures");
    }
  }
}
