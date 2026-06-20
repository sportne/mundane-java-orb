package io.github.mundanej.mjo.security;

import java.util.List;
import java.util.Objects;

/** Validation-only model for bounded CSIv2 metadata snapshots. */
public final class SecurityCsiv2MetadataModel {

  private final SecurityCsiv2MetadataOptions options;

  /** Creates a CSIv2 metadata model with default bounds. */
  public SecurityCsiv2MetadataModel() {
    this(SecurityCsiv2MetadataOptions.defaults());
  }

  /** Creates a CSIv2 metadata model with caller-provided bounds. */
  public SecurityCsiv2MetadataModel(SecurityCsiv2MetadataOptions options) {
    this.options = Objects.requireNonNull(options, "options");
  }

  /** Returns the default supported local CSIv2 metadata snapshot. */
  public SecurityCsiv2MetadataSnapshot defaults() {
    return validate(List.of(SecurityCsiv2Mechanism.defaults()));
  }

  /** Validates mechanisms and returns an immutable deterministic snapshot. */
  public SecurityCsiv2MetadataSnapshot validate(List<SecurityCsiv2Mechanism> mechanisms) {
    if (mechanisms == null || mechanisms.isEmpty()) {
      throw malformed("CSIv2 metadata must contain at least one mechanism");
    }
    if (mechanisms.size() > options.maxMechanisms()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.CSIV2_METADATA_LIMIT_EXCEEDED,
          "CSIv2 mechanism count exceeds " + options.maxMechanisms());
    }
    return new SecurityCsiv2MetadataSnapshot(mechanisms);
  }

  private static SecurityServiceException malformed(String message) {
    return new SecurityServiceException(
        SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA, message);
  }
}
