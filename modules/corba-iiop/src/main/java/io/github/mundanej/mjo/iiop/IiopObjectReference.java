package io.github.mundanej.mjo.iiop;

import io.github.mundanej.mjo.ior.IiopProfile;
import io.github.mundanej.mjo.ior.IiopVersion;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.IorCodeSetComponent;
import io.github.mundanej.mjo.ior.ObjectKey;
import io.github.mundanej.mjo.ior.TaggedProfile;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Network IIOP object reference for a local ORB/POA object. */
public final class IiopObjectReference {

  private final Ior ior;
  private final IiopEndpoint endpoint;
  private final byte[] objectKey;

  /** Creates a validated network object reference. */
  public IiopObjectReference(Ior ior, IiopEndpoint endpoint, byte[] objectKey) {
    this.ior = Objects.requireNonNull(ior, "ior");
    this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
    this.objectKey = Objects.requireNonNull(objectKey, "objectKey").clone();
  }

  /** Creates a network IOR for an existing local ORB object reference. */
  public static IiopObjectReference fromLocal(
      IiopEndpoint endpoint, LocalObjectReference<?> reference) {
    Objects.requireNonNull(reference, "reference");
    byte[] objectKey = objectKeyFor(reference);
    IiopProfile profile =
        new IiopProfile(
            IiopVersion.V1_2,
            endpoint.host(),
            endpoint.port(),
            new ObjectKey(objectKey),
            List.of(IorCodeSetComponent.defaults().toComponent()));
    Ior ior =
        new Ior(
            reference.descriptor().repositoryId().value(),
            List.of(TaggedProfile.internetIop(profile)));
    return new IiopObjectReference(ior, endpoint, objectKey);
  }

  /** Extracts the first IIOP profile from an IOR. */
  public static IiopObjectReference fromIor(Ior ior) {
    Objects.requireNonNull(ior, "ior");
    for (TaggedProfile profile : ior.profiles()) {
      var internetProfile = profile.internetIopProfile();
      if (internetProfile.isPresent()) {
        IiopProfile decoded = internetProfile.orElseThrow();
        return new IiopObjectReference(
            ior, new IiopEndpoint(decoded.host(), decoded.port()), decoded.objectKey().octets());
      }
    }
    throw new IiopException(
        IiopDiagnosticCodes.UNSUPPORTED_MESSAGE, "IOR does not contain an IIOP profile");
  }

  /** Returns the object reference IOR. */
  public Ior ior() {
    return ior;
  }

  /** Returns the endpoint from the selected IIOP profile. */
  public IiopEndpoint endpoint() {
    return endpoint;
  }

  /** Returns the object key. */
  public byte[] objectKey() {
    return objectKey.clone();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof IiopObjectReference that)) {
      return false;
    }
    return ior.equals(that.ior)
        && endpoint.equals(that.endpoint)
        && Arrays.equals(objectKey, that.objectKey);
  }

  @Override
  public int hashCode() {
    return 31 * Objects.hash(ior, endpoint) + Arrays.hashCode(objectKey);
  }

  static byte[] objectKeyFor(String objectId) {
    Objects.requireNonNull(objectId, "objectId");
    if (objectId.isBlank()) {
      throw new IiopException(IiopDiagnosticCodes.UNSUPPORTED_MESSAGE, "object id is blank");
    }
    if (!StandardCharsets.US_ASCII.newEncoder().canEncode(objectId)) {
      throw new IiopException(
          IiopDiagnosticCodes.UNSUPPORTED_MESSAGE, "object id must be US-ASCII");
    }
    return objectId.getBytes(StandardCharsets.US_ASCII);
  }

  static byte[] objectKeyFor(LocalObjectReference<?> reference) {
    Objects.requireNonNull(reference, "reference");
    return reference
        .durableObjectKey()
        .map(key -> key.encode())
        .orElseGet(() -> objectKeyFor(reference.objectId()));
  }
}
