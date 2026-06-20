package io.github.mundanej.mjo.security;

import java.util.List;

/** Immutable CSIv2 metadata snapshot for the supported local subset. */
public record SecurityCsiv2MetadataSnapshot(List<SecurityCsiv2Mechanism> mechanisms) {

  /** Creates a validated immutable metadata snapshot. */
  public SecurityCsiv2MetadataSnapshot {
    if (mechanisms == null || mechanisms.isEmpty()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA,
          "CSIv2 metadata must contain at least one mechanism");
    }
    if (mechanisms.size() > SecurityCsiv2MetadataOptions.ABSOLUTE_MAX_MECHANISMS) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.CSIV2_METADATA_LIMIT_EXCEEDED,
          "CSIv2 metadata exceeds the absolute mechanism limit");
    }
    for (SecurityCsiv2Mechanism mechanism : mechanisms) {
      if (mechanism == null) {
        throw new SecurityServiceException(
            SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA,
            "CSIv2 mechanism must not be null");
      }
    }
    mechanisms = List.copyOf(mechanisms);
  }
}
