package io.github.mundanej.mjo.security;

/** Caller-configurable bounds for CSIv2 metadata validation and text encoding. */
public record SecurityCsiv2MetadataOptions(int maxMechanisms, int maxEncodedLength) {

  /** Default number of mechanisms accepted in one metadata snapshot. */
  public static final int DEFAULT_MAX_MECHANISMS = 4;

  /** Default upper bound for deterministic encoded metadata text. */
  public static final int DEFAULT_MAX_ENCODED_LENGTH = 1024;

  /** Absolute upper bound for mechanism count configuration. */
  public static final int ABSOLUTE_MAX_MECHANISMS = 32;

  /** Absolute upper bound for encoded metadata text configuration. */
  public static final int ABSOLUTE_MAX_ENCODED_LENGTH = 16_384;

  /** Creates validated CSIv2 metadata bounds. */
  public SecurityCsiv2MetadataOptions {
    requireRange(maxMechanisms, 1, ABSOLUTE_MAX_MECHANISMS, "CSIv2 mechanism limit");
    requireRange(maxEncodedLength, 128, ABSOLUTE_MAX_ENCODED_LENGTH, "CSIv2 encoded length limit");
  }

  /** Returns default CSIv2 metadata bounds. */
  public static SecurityCsiv2MetadataOptions defaults() {
    return new SecurityCsiv2MetadataOptions(DEFAULT_MAX_MECHANISMS, DEFAULT_MAX_ENCODED_LENGTH);
  }

  private static void requireRange(int value, int min, int max, String label) {
    if (value < min || value > max) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.INVALID_LIMIT,
          label + " must be between " + min + " and " + max);
    }
  }
}
