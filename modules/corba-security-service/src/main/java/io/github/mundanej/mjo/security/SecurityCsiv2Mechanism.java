package io.github.mundanej.mjo.security;

/** One supported CSIv2 mechanism advertisement for local metadata validation. */
public record SecurityCsiv2Mechanism(
    String mechanismId,
    SecurityTransportProtection transportProtection,
    SecurityCsiv2IdentityTokenPolicy identityTokenPolicy,
    SecurityAuthenticationRequirement targetAuthentication,
    SecurityAuthenticationRequirement clientAuthentication) {

  /** Creates a validated CSIv2 mechanism advertisement. */
  public SecurityCsiv2Mechanism {
    mechanismId = SecurityCsiv2MechanismIds.requireSupported(mechanismId);
    if (transportProtection == null
        || identityTokenPolicy == null
        || targetAuthentication == null
        || clientAuthentication == null) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA,
          "CSIv2 mechanism metadata must be complete");
    }
  }

  /** Creates the default mechanism for the supported local metadata subset. */
  public static SecurityCsiv2Mechanism defaults() {
    return new SecurityCsiv2Mechanism(
        SecurityCsiv2MechanismIds.SUPPORTED_LOCAL,
        SecurityTransportProtection.NONE,
        SecurityCsiv2IdentityTokenPolicy.ABSENT,
        SecurityAuthenticationRequirement.OPTIONAL,
        SecurityAuthenticationRequirement.OPTIONAL);
  }
}
