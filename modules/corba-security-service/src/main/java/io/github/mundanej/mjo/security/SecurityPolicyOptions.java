package io.github.mundanej.mjo.security;

/** Caller-provided local Security Service policy validation limits. */
public record SecurityPolicyOptions(int maxPolicySettings) {

  /** Default maximum number of policy settings accepted in one validation request. */
  public static final int DEFAULT_MAX_POLICY_SETTINGS = 16;

  /** Maximum supported bound for policy validation limits. */
  public static final int MAX_SUPPORTED_LIMIT = 1_024;

  /** Creates validated policy options. */
  public SecurityPolicyOptions {
    if (maxPolicySettings < 1 || maxPolicySettings > MAX_SUPPORTED_LIMIT) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.INVALID_LIMIT,
          "maxPolicySettings must be between 1 and " + MAX_SUPPORTED_LIMIT);
    }
  }

  /** Returns default bounded policy validation options. */
  public static SecurityPolicyOptions defaults() {
    return new SecurityPolicyOptions(DEFAULT_MAX_POLICY_SETTINGS);
  }
}
