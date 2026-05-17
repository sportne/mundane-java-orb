package io.github.mundanej.mjo.ior;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrEncapsulation;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Decoded TAG_INTERNET_IOP profile body. */
public final class IiopProfile {

  private final IiopVersion version;
  private final String host;
  private final int port;
  private final ObjectKey objectKey;
  private final List<TaggedComponent> components;
  private final byte[] trailingData;

  /** Creates an IIOP profile with default bounds and no trailing extension bytes. */
  public IiopProfile(
      IiopVersion version,
      String host,
      int port,
      ObjectKey objectKey,
      List<TaggedComponent> components) {
    this(version, host, port, objectKey, components, new byte[0], IorLimits.defaults());
  }

  /** Creates an IIOP profile with caller-supplied bounds and no trailing extension bytes. */
  public IiopProfile(
      IiopVersion version,
      String host,
      int port,
      ObjectKey objectKey,
      List<TaggedComponent> components,
      IorLimits limits) {
    this(version, host, port, objectKey, components, new byte[0], limits);
  }

  /** Creates an IIOP profile with caller-supplied bounds. */
  public IiopProfile(
      IiopVersion version,
      String host,
      int port,
      ObjectKey objectKey,
      List<TaggedComponent> components,
      byte[] trailingData,
      IorLimits limits) {
    this.version = requireSupported(Objects.requireNonNull(version, "version"));
    this.host = requireProfileHost(host);
    this.port = IorWire.requireUnsignedShort(port, "IIOP port");
    this.objectKey = Objects.requireNonNull(objectKey, "objectKey");
    Objects.requireNonNull(components, "components");
    limits.requireWithin(limits.componentCount(), components.size());
    if (!version.carriesTaggedComponents() && !components.isEmpty()) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_IIOP_PROFILE,
          "IIOP 1.0 profiles cannot carry tagged components");
    }
    if (!version.carriesTaggedComponents() && trailingData.length != 0) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_IIOP_PROFILE,
          "IIOP 1.0 profiles cannot carry trailing extension data");
    }
    this.components = List.copyOf(components);
    this.trailingData =
        Arrays.copyOf(Objects.requireNonNull(trailingData, "trailingData"), trailingData.length);
  }

  /** Decodes a TAG_INTERNET_IOP profile body with default bounds. */
  public static IiopProfile fromProfileData(byte[] profileData) {
    return fromProfileData(profileData, IorLimits.defaults());
  }

  /** Decodes a TAG_INTERNET_IOP profile body with caller-supplied bounds. */
  public static IiopProfile fromProfileData(byte[] profileData, IorLimits limits) {
    Objects.requireNonNull(profileData, "profileData");
    CdrReader reader = CdrEncapsulation.fromBytes(profileData).reader(limits.cdrLimits());
    IiopVersion version = new IiopVersion(reader.readOctet(), reader.readOctet());
    requireSupported(version);
    String host = reader.readString();
    int port = reader.readUnsignedShort();
    ObjectKey objectKey = new ObjectKey(reader.readOctetSequence(), limits);
    List<TaggedComponent> components = List.of();
    byte[] trailing = new byte[0];
    if (version.carriesTaggedComponents()) {
      components = readComponents(reader, limits);
      trailing = readRemainingOctets(reader);
    } else if (reader.remaining() != 0) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_IIOP_PROFILE,
          "IIOP 1.0 profile data must end after object_key");
    }
    return new IiopProfile(version, host, port, objectKey, components, trailing, limits);
  }

  /** Returns the IIOP profile version. */
  public IiopVersion version() {
    return version;
  }

  /** Returns the host string from the profile. */
  public String host() {
    return host;
  }

  /** Returns the unsigned TCP port. */
  public int port() {
    return port;
  }

  /** Returns the opaque object key. */
  public ObjectKey objectKey() {
    return objectKey;
  }

  /** Returns the tagged components carried by IIOP 1.1 and later profiles. */
  public List<TaggedComponent> components() {
    return components;
  }

  /** Returns trailing extension bytes for IIOP minor versions beyond the known structure. */
  public byte[] trailingData() {
    return Arrays.copyOf(trailingData, trailingData.length);
  }

  /** Encodes this profile body as TAG_INTERNET_IOP profile data. */
  public byte[] toProfileData() {
    CdrWriter writer = new CdrWriter(CdrByteOrder.BIG_ENDIAN);
    writer.writeOctet(0);
    writer.writeOctet(version.major()).writeOctet(version.minor());
    writer.writeString(host);
    writer.writeUnsignedShort(port);
    writer.writeOctetSequence(objectKey.octets());
    if (version.carriesTaggedComponents()) {
      writer.writeSequenceLength(components.size());
      for (TaggedComponent component : components) {
        component.writeTo(writer);
      }
      for (byte octet : trailingData) {
        writer.writeOctet(octet & 0xFF);
      }
    }
    return writer.toByteArray();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof IiopProfile that)) {
      return false;
    }
    return version.equals(that.version)
        && host.equals(that.host)
        && port == that.port
        && objectKey.equals(that.objectKey)
        && components.equals(that.components)
        && Arrays.equals(trailingData, that.trailingData);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(version, host, port, objectKey, components);
    return 31 * result + Arrays.hashCode(trailingData);
  }

  @Override
  public String toString() {
    return "IiopProfile[version="
        + version
        + ", host="
        + host
        + ", port="
        + port
        + ", objectKeyOctets="
        + objectKey.octets().length
        + ", components="
        + components.size()
        + "]";
  }

  private static List<TaggedComponent> readComponents(CdrReader reader, IorLimits limits) {
    int componentCount = reader.readSequenceLength();
    limits.requireWithin(limits.componentCount(), componentCount);
    List<TaggedComponent> components = new ArrayList<>(componentCount);
    for (int index = 0; index < componentCount; index++) {
      components.add(TaggedComponent.readFrom(reader, limits));
    }
    return components;
  }

  private static byte[] readRemainingOctets(CdrReader reader) {
    byte[] octets = new byte[reader.remaining()];
    for (int index = 0; index < octets.length; index++) {
      octets[index] = (byte) reader.readOctet();
    }
    return octets;
  }

  private static IiopVersion requireSupported(IiopVersion version) {
    if (version.major() != 1) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_IIOP_PROFILE,
          "only IIOP major version 1 is supported in this slice: " + version);
    }
    return version;
  }

  private static String requireProfileHost(String host) {
    Objects.requireNonNull(host, "host");
    if (host.isBlank()) {
      throw new IorException(IorDiagnosticCodes.INVALID_IIOP_PROFILE, "IIOP host is required");
    }
    return host;
  }
}
