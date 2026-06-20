package io.github.mundanej.mjo.security;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Codec for CSIv2 metadata carried in local IIOP service contexts and tagged components. */
public final class SecurityIiopContextCodec {

  /** Maximum IIOP security descriptors accepted by this local boundary. */
  public static final int MAX_CONTEXT_DESCRIPTORS = 64;

  private final SecurityCsiv2MetadataCodec metadataCodec;

  /** Creates a codec backed by the default CSIv2 metadata codec. */
  public SecurityIiopContextCodec() {
    this(new SecurityCsiv2MetadataCodec());
  }

  /** Creates a codec backed by a caller-provided CSIv2 metadata codec. */
  public SecurityIiopContextCodec(SecurityCsiv2MetadataCodec metadataCodec) {
    this.metadataCodec = Objects.requireNonNull(metadataCodec, "metadataCodec");
  }

  /** Encodes metadata as a local IIOP service-context descriptor. */
  public SecurityIiopContextDescriptor encodeServiceContext(
      SecurityCsiv2MetadataSnapshot metadata) {
    return encode(
        SecurityIiopContextKind.SERVICE_CONTEXT,
        SecurityIiopContextDescriptor.SECURITY_SERVICE_CONTEXT_ID,
        metadata);
  }

  /** Encodes metadata as a local IOR tagged-component descriptor. */
  public SecurityIiopContextDescriptor encodeTaggedComponent(
      SecurityCsiv2MetadataSnapshot metadata) {
    return encode(
        SecurityIiopContextKind.TAGGED_COMPONENT,
        SecurityIiopContextDescriptor.SECURITY_TAGGED_COMPONENT_ID,
        metadata);
  }

  /** Decodes local CSIv2 metadata from a Security Service IIOP descriptor. */
  public SecurityCsiv2MetadataSnapshot decode(SecurityIiopContextDescriptor descriptor) {
    Objects.requireNonNull(descriptor, "descriptor");
    if (!descriptor.isSecurityServiceContext() && !descriptor.isSecurityTaggedComponent()) {
      throw malformed("descriptor is not a Security Service IIOP context");
    }
    try {
      String encoded =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(descriptor.contextData()))
              .toString();
      return metadataCodec.decode(encoded);
    } catch (CharacterCodingException | SecurityServiceException exception) {
      throw malformed("malformed Security Service IIOP context");
    }
  }

  /** Finds the single local Security Service service context in a bounded list. */
  public Optional<SecurityIiopContextDescriptor> findServiceContext(
      List<SecurityIiopContextDescriptor> descriptors) {
    return find(descriptors, SecurityIiopContextDescriptor::isSecurityServiceContext);
  }

  /** Finds the single local Security Service tagged component in a bounded list. */
  public Optional<SecurityIiopContextDescriptor> findTaggedComponent(
      List<SecurityIiopContextDescriptor> descriptors) {
    return find(descriptors, SecurityIiopContextDescriptor::isSecurityTaggedComponent);
  }

  private SecurityIiopContextDescriptor encode(
      SecurityIiopContextKind kind, long id, SecurityCsiv2MetadataSnapshot metadata) {
    String encoded = metadataCodec.encode(metadata);
    return new SecurityIiopContextDescriptor(kind, id, encoded.getBytes(StandardCharsets.UTF_8));
  }

  private Optional<SecurityIiopContextDescriptor> find(
      List<SecurityIiopContextDescriptor> descriptors, ContextMatcher matcher) {
    if (descriptors == null) {
      throw malformed("IIOP security context list must not be null");
    }
    if (descriptors.size() > MAX_CONTEXT_DESCRIPTORS) {
      throw malformed("IIOP security context list exceeds " + MAX_CONTEXT_DESCRIPTORS + " entries");
    }
    SecurityIiopContextDescriptor found = null;
    for (SecurityIiopContextDescriptor descriptor : descriptors) {
      if (descriptor == null) {
        throw malformed("IIOP security context list must not contain null entries");
      }
      if (matcher.matches(descriptor)) {
        if (found != null) {
          throw malformed("duplicate Security Service IIOP context");
        }
        found = descriptor;
      }
    }
    return Optional.ofNullable(found);
  }

  private static SecurityServiceException malformed(String message) {
    return new SecurityServiceException(
        SecurityServiceDiagnosticCodes.MALFORMED_IIOP_SECURITY_CONTEXT, message);
  }

  private interface ContextMatcher {
    boolean matches(SecurityIiopContextDescriptor descriptor);
  }
}
