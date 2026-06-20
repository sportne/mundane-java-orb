package io.github.mundanej.mjo.security;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Builds bounded redacted audit events and failure disclosures. */
public final class SecurityAuditDisclosureModel {

  private final SecurityAuditOptions options;
  private final Clock clock;

  /** Creates a model with default audit bounds and system UTC clock. */
  public SecurityAuditDisclosureModel() {
    this(SecurityAuditOptions.defaults(), Clock.systemUTC());
  }

  /** Creates a model with caller-provided bounds and clock. */
  public SecurityAuditDisclosureModel(SecurityAuditOptions options, Clock clock) {
    this.options = Objects.requireNonNull(options, "options");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /** Creates a redacted audit event from a policy decision and caller-supplied fields. */
  public SecurityAuditEvent event(
      SecurityPolicyEvaluationDecision decision, List<SecurityAuditField> fields) {
    Objects.requireNonNull(decision, "decision");
    List<SecurityAuditField> safeFields = sortedFields(fields);
    List<SecurityFailureDisclosure> failures =
        decision.reasons().stream()
            .map(SecurityFailureDisclosure::from)
            .sorted(Comparator.comparing(disclosure -> disclosure.code().value()))
            .toList();
    if (failures.size() > options.maxFailures()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.AUDIT_EVENT_LIMIT_EXCEEDED,
          "audit event failure disclosures exceed " + options.maxFailures());
    }
    return new SecurityAuditEvent(
        SecurityAuditEventType.POLICY_EVALUATION,
        clock.instant(),
        decision.status(),
        failures,
        safeFields);
  }

  /** Creates a redacted failure disclosure without exposing exception messages. */
  public SecurityFailureDisclosure disclose(SecurityServiceException exception) {
    return SecurityFailureDisclosure.from(exception);
  }

  private List<SecurityAuditField> sortedFields(List<SecurityAuditField> fields) {
    if (fields == null) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_AUDIT_EVENT,
          "audit event fields must not be null");
    }
    if (fields.size() > options.maxFields()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.AUDIT_EVENT_LIMIT_EXCEEDED,
          "audit event fields exceed " + options.maxFields());
    }
    if (fields.stream().anyMatch(Objects::isNull)) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_AUDIT_EVENT,
          "audit event fields must not contain null entries");
    }
    return fields.stream()
        .sorted(
            Comparator.comparing(SecurityAuditField::key).thenComparing(SecurityAuditField::value))
        .toList();
  }
}
