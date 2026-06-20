package io.github.mundanej.mjo.security;

import io.github.mundanej.mjo.common.DiagnosticCode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Local policy evaluator for the supported Security Service subset. */
public final class SecurityPolicyEvaluator {

  private final LocalSecurityTrustModel trustModel;
  private final Clock clock;

  /** Creates an evaluator with a fresh local trust model and system UTC clock. */
  public SecurityPolicyEvaluator() {
    this(new LocalSecurityTrustModel(), Clock.systemUTC());
  }

  /** Creates an evaluator with a caller-provided trust model and system UTC clock. */
  public SecurityPolicyEvaluator(LocalSecurityTrustModel trustModel) {
    this(trustModel, Clock.systemUTC());
  }

  /** Creates an evaluator with caller-provided trust model and clock. */
  public SecurityPolicyEvaluator(LocalSecurityTrustModel trustModel, Clock clock) {
    this.trustModel = Objects.requireNonNull(trustModel, "trustModel");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /** Evaluates one local request and returns a deterministic decision. */
  public SecurityPolicyEvaluationDecision evaluate(SecurityPolicyEvaluationRequest request) {
    Objects.requireNonNull(request, "request");
    List<SecurityPolicyEvaluationReason> reasons = new ArrayList<>();
    SecurityPolicySnapshot policy = request.policy();
    if (policy == null) {
      reasons.add(reason(SecurityServiceDiagnosticCodes.MALFORMED_POLICY, "policy is missing"));
      return deny(reasons);
    }

    addPolicyReasons(policy, reasons);
    addMetadataReasons(policy, request.csiv2Metadata(), reasons);
    addCredentialReasons(policy, request.credential(), reasons);
    if (reasons.isEmpty()) {
      return SecurityPolicyEvaluationDecision.allow();
    }
    if (reasons.stream()
        .allMatch(
            reason -> reason.code().equals(SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL))) {
      return new SecurityPolicyEvaluationDecision(
          SecurityPolicyEvaluationStatus.CHALLENGE, reasons);
    }
    return deny(reasons);
  }

  private static SecurityPolicyEvaluationDecision deny(
      List<SecurityPolicyEvaluationReason> reasons) {
    return new SecurityPolicyEvaluationDecision(SecurityPolicyEvaluationStatus.DENY, reasons);
  }

  private static void addPolicyReasons(
      SecurityPolicySnapshot policy, List<SecurityPolicyEvaluationReason> reasons) {
    if (policy.delegation() == SecurityDelegationMode.ENABLED) {
      reasons.add(
          reason(
              SecurityServiceDiagnosticCodes.UNSUPPORTED_DELEGATION,
              "delegation is outside the supported local subset"));
    }
    if (policy.trust() == SecurityTrustRequirement.REQUIRED
        && policy.authentication() == SecurityAuthenticationRequirement.OPTIONAL) {
      reasons.add(
          reason(
              SecurityServiceDiagnosticCodes.CONFLICTING_POLICY,
              "trust-required policy requires authentication-required policy"));
    }
    if (policy.identityAssertion() == SecurityIdentityAssertionMode.SELF
        && policy.authentication() == SecurityAuthenticationRequirement.OPTIONAL) {
      reasons.add(
          reason(
              SecurityServiceDiagnosticCodes.CONFLICTING_POLICY,
              "identity assertion requires authentication-required policy"));
    }
  }

  private static void addMetadataReasons(
      SecurityPolicySnapshot policy,
      SecurityCsiv2MetadataSnapshot metadata,
      List<SecurityPolicyEvaluationReason> reasons) {
    if (metadata == null) {
      reasons.add(
          reason(
              SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA,
              "CSIv2 metadata is missing"));
      return;
    }
    boolean hasTransport =
        metadata.mechanisms().stream()
            .anyMatch(
                mechanism ->
                    protectionRank(mechanism.transportProtection())
                        >= protectionRank(policy.transportProtection()));
    if (!hasTransport) {
      reasons.add(
          reason(
              SecurityServiceDiagnosticCodes.INSUFFICIENT_TRANSPORT_PROTECTION,
              "CSIv2 metadata does not satisfy the required transport protection"));
    }
    if (policy.identityAssertion() == SecurityIdentityAssertionMode.SELF) {
      boolean hasPrincipalToken =
          metadata.mechanisms().stream()
              .anyMatch(
                  mechanism ->
                      mechanism.identityTokenPolicy()
                          == SecurityCsiv2IdentityTokenPolicy.PRINCIPAL_NAME);
      if (!hasPrincipalToken) {
        reasons.add(
            reason(
                SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA,
                "CSIv2 metadata does not advertise principal-name identity tokens"));
      }
    }
  }

  private void addCredentialReasons(
      SecurityPolicySnapshot policy,
      SecurityTrustEvaluationInput credential,
      List<SecurityPolicyEvaluationReason> reasons) {
    boolean needsCredential =
        policy.authentication() == SecurityAuthenticationRequirement.REQUIRED
            || policy.trust() == SecurityTrustRequirement.REQUIRED
            || policy.identityAssertion() == SecurityIdentityAssertionMode.SELF;
    if (needsCredential && credential == null) {
      reasons.add(
          reason(SecurityServiceDiagnosticCodes.MISSING_CREDENTIAL, "credential is required"));
      return;
    }
    if (credential == null) {
      return;
    }
    SecurityServiceException lifetimeFailure = validateLifetime(credential);
    if (lifetimeFailure != null) {
      reasons.add(reason(lifetimeFailure.code(), "credential is not valid for evaluation"));
      return;
    }
    if (policy.trust() == SecurityTrustRequirement.REQUIRED) {
      try {
        trustModel.evaluate(credential);
      } catch (SecurityServiceException exception) {
        if (isTrustFailure(exception)) {
          reasons.add(reason(exception.code(), "credential is not trusted for local policy"));
          return;
        }
        throw exception;
      }
    }
  }

  private SecurityServiceException validateLifetime(SecurityTrustEvaluationInput credential) {
    try {
      SecurityLifetimes.requireValidAt(credential, clock.instant());
      return null;
    } catch (SecurityServiceException exception) {
      if (exception.code().equals(SecurityServiceDiagnosticCodes.CREDENTIAL_EXPIRED)) {
        return exception;
      }
      throw exception;
    }
  }

  private static boolean isTrustFailure(SecurityServiceException exception) {
    return exception.code().equals(SecurityServiceDiagnosticCodes.CREDENTIAL_EXPIRED)
        || exception.code().equals(SecurityServiceDiagnosticCodes.CREDENTIAL_UNTRUSTED);
  }

  private static int protectionRank(SecurityTransportProtection protection) {
    return switch (protection) {
      case NONE -> 0;
      case INTEGRITY -> 1;
      case CONFIDENTIALITY -> 2;
    };
  }

  private static SecurityPolicyEvaluationReason reason(DiagnosticCode code, String message) {
    return new SecurityPolicyEvaluationReason(code, message);
  }
}
