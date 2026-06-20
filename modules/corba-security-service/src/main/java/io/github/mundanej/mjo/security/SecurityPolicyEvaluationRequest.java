package io.github.mundanej.mjo.security;

/** Input for local Security Service policy evaluation. */
public record SecurityPolicyEvaluationRequest(
    SecurityPolicySnapshot policy,
    SecurityCsiv2MetadataSnapshot csiv2Metadata,
    SecurityTrustEvaluationInput credential) {

  /** Creates an anonymous local evaluation request. */
  public static SecurityPolicyEvaluationRequest anonymous(
      SecurityPolicySnapshot policy, SecurityCsiv2MetadataSnapshot csiv2Metadata) {
    return new SecurityPolicyEvaluationRequest(policy, csiv2Metadata, null);
  }

  /** Creates a credential-bearing local evaluation request. */
  public static SecurityPolicyEvaluationRequest authenticated(
      SecurityPolicySnapshot policy,
      SecurityCsiv2MetadataSnapshot csiv2Metadata,
      SecurityTrustEvaluationInput credential) {
    return new SecurityPolicyEvaluationRequest(policy, csiv2Metadata, credential);
  }
}
