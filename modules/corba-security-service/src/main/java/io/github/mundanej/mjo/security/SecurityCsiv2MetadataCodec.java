package io.github.mundanej.mjo.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/** Deterministic text codec for project-owned CSIv2 metadata. */
public final class SecurityCsiv2MetadataCodec {

  /** Stable codec version prefix for the local CSIv2 metadata subset. */
  public static final String VERSION = "mjo-csiv2-meta-v1";

  private static final int MECHANISM_FIELD_COUNT = 5;
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private final SecurityCsiv2MetadataOptions options;

  /** Creates a codec with default metadata bounds. */
  public SecurityCsiv2MetadataCodec() {
    this(SecurityCsiv2MetadataOptions.defaults());
  }

  /** Creates a codec with caller-provided metadata bounds. */
  public SecurityCsiv2MetadataCodec(SecurityCsiv2MetadataOptions options) {
    this.options = Objects.requireNonNull(options, "options");
  }

  /** Encodes a CSIv2 metadata snapshot into bounded deterministic text. */
  public String encode(SecurityCsiv2MetadataSnapshot snapshot) {
    if (snapshot == null) {
      throw malformed("CSIv2 metadata snapshot must not be null");
    }
    SecurityCsiv2MetadataSnapshot bounded =
        new SecurityCsiv2MetadataModel(options).validate(snapshot.mechanisms());
    StringBuilder builder =
        new StringBuilder(VERSION).append('|').append(bounded.mechanisms().size());
    for (SecurityCsiv2Mechanism mechanism : bounded.mechanisms()) {
      builder
          .append('|')
          .append(ENCODER.encodeToString(mechanism.mechanismId().getBytes(StandardCharsets.UTF_8)))
          .append(':')
          .append(mechanism.transportProtection())
          .append(':')
          .append(mechanism.identityTokenPolicy())
          .append(':')
          .append(mechanism.targetAuthentication())
          .append(':')
          .append(mechanism.clientAuthentication());
    }
    String encoded = builder.toString();
    if (encoded.length() > options.maxEncodedLength()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.CSIV2_METADATA_LIMIT_EXCEEDED,
          "CSIv2 metadata exceeds " + options.maxEncodedLength() + " characters");
    }
    return encoded;
  }

  /** Decodes bounded deterministic text into a CSIv2 metadata snapshot. */
  public SecurityCsiv2MetadataSnapshot decode(String encoded) {
    if (encoded == null || encoded.isBlank()) {
      throw malformed("CSIv2 metadata text must not be blank");
    }
    if (encoded.length() > options.maxEncodedLength()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.CSIV2_METADATA_LIMIT_EXCEEDED,
          "CSIv2 metadata exceeds " + options.maxEncodedLength() + " characters");
    }
    String[] fields = encoded.split("\\|", -1);
    if (fields.length < 3) {
      throw malformed("CSIv2 metadata must include version, count, and mechanisms");
    }
    if (!VERSION.equals(fields[0])) {
      throw malformed("unsupported CSIv2 metadata version");
    }
    int count = parseCount(fields[1]);
    if (fields.length != count + 2) {
      throw malformed("CSIv2 mechanism count does not match encoded fields");
    }
    if (count > options.maxMechanisms()) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.CSIV2_METADATA_LIMIT_EXCEEDED,
          "CSIv2 mechanism count exceeds " + options.maxMechanisms());
    }
    List<SecurityCsiv2Mechanism> mechanisms = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      mechanisms.add(decodeMechanism(fields[i + 2]));
    }
    return new SecurityCsiv2MetadataModel(options).validate(mechanisms);
  }

  private static int parseCount(String value) {
    try {
      int count = Integer.parseInt(value);
      if (count < 1) {
        throw malformed("CSIv2 mechanism count must be positive");
      }
      return count;
    } catch (NumberFormatException exception) {
      throw malformed("malformed CSIv2 mechanism count");
    }
  }

  private static SecurityCsiv2Mechanism decodeMechanism(String encoded) {
    String[] fields = encoded.split(":", -1);
    if (fields.length != MECHANISM_FIELD_COUNT) {
      throw malformed("CSIv2 mechanism must contain " + MECHANISM_FIELD_COUNT + " fields");
    }
    try {
      String mechanismId = new String(DECODER.decode(fields[0]), StandardCharsets.UTF_8);
      return new SecurityCsiv2Mechanism(
          mechanismId,
          SecurityTransportProtection.valueOf(fields[1]),
          SecurityCsiv2IdentityTokenPolicy.valueOf(fields[2]),
          SecurityAuthenticationRequirement.valueOf(fields[3]),
          SecurityAuthenticationRequirement.valueOf(fields[4]));
    } catch (IllegalArgumentException exception) {
      throw malformed("malformed CSIv2 mechanism field");
    }
  }

  private static SecurityServiceException malformed(String message) {
    return new SecurityServiceException(
        SecurityServiceDiagnosticCodes.MALFORMED_CSIV2_METADATA, message);
  }
}
