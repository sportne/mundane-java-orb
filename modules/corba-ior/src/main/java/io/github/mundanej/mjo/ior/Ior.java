package io.github.mundanej.mjo.ior;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrEncapsulation;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable Interoperable Object Reference. */
public record Ior(String typeId, List<TaggedProfile> profiles) {

  /** Creates an IOR with defensive profile storage. */
  public Ior {
    Objects.requireNonNull(typeId, "typeId");
    profiles = List.copyOf(Objects.requireNonNull(profiles, "profiles"));
  }

  /** Returns the standard null object reference IOR. */
  public static Ior nullReference() {
    return new Ior("", List.of());
  }

  /** Decodes an IOR body from CDR bytes with default bounds. */
  public static Ior fromCdrBody(byte[] body) {
    return fromCdrBody(body, IorLimits.defaults());
  }

  /** Decodes an IOR body from CDR bytes with caller-supplied bounds. */
  public static Ior fromCdrBody(byte[] body, IorLimits limits) {
    CdrReader reader = new CdrReader(CdrByteOrder.BIG_ENDIAN, body, limits.cdrLimits());
    Ior ior = readFrom(reader, limits);
    if (reader.remaining() != 0) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_IIOP_PROFILE,
          "IOR body contains trailing octets: " + reader.remaining());
    }
    return ior;
  }

  /** Decodes an encapsulated IOR with default bounds. */
  public static Ior fromEncapsulation(byte[] bytes) {
    return fromEncapsulation(bytes, IorLimits.defaults());
  }

  /** Decodes an encapsulated IOR with caller-supplied bounds. */
  public static Ior fromEncapsulation(byte[] bytes, IorLimits limits) {
    CdrReader reader = CdrEncapsulation.fromBytes(bytes).reader(limits.cdrLimits());
    Ior ior = readFrom(reader, limits);
    if (reader.remaining() != 0) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_IIOP_PROFILE,
          "IOR encapsulation contains trailing octets: " + reader.remaining());
    }
    return ior;
  }

  /** Reads one IOR value from a CDR reader. */
  public static Ior readFrom(CdrReader reader, IorLimits limits) {
    String typeId = reader.readString();
    int profileCount = reader.readSequenceLength();
    limits.requireWithin(limits.profileCount(), profileCount);
    List<TaggedProfile> profiles = new ArrayList<>(profileCount);
    for (int index = 0; index < profileCount; index++) {
      profiles.add(TaggedProfile.readFrom(reader, limits));
    }
    return new Ior(typeId, profiles);
  }

  /** Encodes this IOR as CDR body bytes. */
  public byte[] toCdrBody() {
    CdrWriter writer = new CdrWriter(CdrByteOrder.BIG_ENDIAN);
    writeTo(writer);
    return writer.toByteArray();
  }

  /** Encodes this IOR as a CDR encapsulation. */
  public byte[] toEncapsulation() {
    CdrWriter writer = new CdrWriter(CdrByteOrder.BIG_ENDIAN);
    writer.writeOctet(0);
    writeTo(writer);
    return writer.toByteArray();
  }

  /** Writes this IOR to a CDR writer. */
  public void writeTo(CdrWriter writer) {
    writer.writeString(typeId).writeSequenceLength(profiles.size());
    for (TaggedProfile profile : profiles) {
      profile.writeTo(writer);
    }
  }
}
