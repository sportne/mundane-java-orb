package io.github.mundanej.mjo.security;

import java.util.Arrays;

/** Immutable descriptor for local CSIv2 metadata carried by IIOP containers. */
public final class SecurityIiopContextDescriptor {

  /** Project-owned service-context ID for the supported local Security Service subset. */
  public static final long SECURITY_SERVICE_CONTEXT_ID = 0x4D4A5343L;

  /** Project-owned tagged-component ID for the supported local Security Service subset. */
  public static final long SECURITY_TAGGED_COMPONENT_ID = 0x4D4A5354L;

  private final SecurityIiopContextKind kind;
  private final long contextId;
  private final byte[] contextData;

  /** Creates a bounded IIOP security context descriptor. */
  public SecurityIiopContextDescriptor(
      SecurityIiopContextKind kind, long contextId, byte[] contextData) {
    if (kind == null) {
      throw malformed("IIOP security context kind must not be null");
    }
    if (contextData == null) {
      throw malformed("IIOP security context data must not be null");
    }
    if (contextId < 0 || contextId > 0xFFFF_FFFFL) {
      throw malformed("IIOP security context ID must fit in unsigned 32-bit range");
    }
    byte[] copy = contextData.clone();
    if (copy.length == 0) {
      throw malformed("IIOP security context data must not be empty");
    }
    if (copy.length > SecurityCsiv2MetadataOptions.ABSOLUTE_MAX_ENCODED_LENGTH) {
      throw malformed(
          "IIOP security context exceeds "
              + SecurityCsiv2MetadataOptions.ABSOLUTE_MAX_ENCODED_LENGTH
              + " bytes");
    }
    this.kind = kind;
    this.contextId = contextId;
    this.contextData = copy;
  }

  /** Returns the IIOP carrier kind. */
  public SecurityIiopContextKind kind() {
    return kind;
  }

  /** Returns the service-context ID or tagged-component tag. */
  public long contextId() {
    return contextId;
  }

  /** Returns a defensive copy of the bounded context bytes. */
  public byte[] contextData() {
    return contextData.clone();
  }

  /** Returns whether this descriptor is the local Security Service service context. */
  public boolean isSecurityServiceContext() {
    return kind == SecurityIiopContextKind.SERVICE_CONTEXT
        && contextId == SECURITY_SERVICE_CONTEXT_ID;
  }

  /** Returns whether this descriptor is the local Security Service tagged component. */
  public boolean isSecurityTaggedComponent() {
    return kind == SecurityIiopContextKind.TAGGED_COMPONENT
        && contextId == SECURITY_TAGGED_COMPONENT_ID;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof SecurityIiopContextDescriptor descriptor
        && kind == descriptor.kind
        && contextId == descriptor.contextId
        && Arrays.equals(contextData, descriptor.contextData);
  }

  @Override
  public int hashCode() {
    int result = kind.hashCode();
    result = 31 * result + Long.hashCode(contextId);
    result = 31 * result + Arrays.hashCode(contextData);
    return result;
  }

  @Override
  public String toString() {
    return "SecurityIiopContextDescriptor[kind="
        + kind
        + ", contextId="
        + contextId
        + ", contextDataLength="
        + contextData.length
        + ']';
  }

  private static SecurityServiceException malformed(String message) {
    return new SecurityServiceException(
        SecurityServiceDiagnosticCodes.MALFORMED_IIOP_SECURITY_CONTEXT, message);
  }
}
