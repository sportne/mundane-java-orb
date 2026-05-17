package io.github.mundanej.mjo.ior;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/** Immutable IOP tagged profile. */
public final class TaggedProfile {

  private final long tag;
  private final byte[] profileData;

  /** Creates a tagged profile with default bounds. */
  public TaggedProfile(long tag, byte[] profileData) {
    this(tag, profileData, IorLimits.defaults());
  }

  /** Creates a tagged profile with caller-supplied bounds. */
  public TaggedProfile(long tag, byte[] profileData, IorLimits limits) {
    Objects.requireNonNull(profileData, "profileData");
    this.tag = IorWire.requireUnsignedLong(tag, "profile tag");
    limits.requireWithin(limits.profileDataOctets(), profileData.length);
    this.profileData = Arrays.copyOf(profileData, profileData.length);
  }

  /** Creates a TAG_INTERNET_IOP profile from a decoded IIOP profile. */
  public static TaggedProfile internetIop(IiopProfile profile) {
    Objects.requireNonNull(profile, "profile");
    return new TaggedProfile(IorTags.TAG_INTERNET_IOP, profile.toProfileData());
  }

  /** Reads one tagged profile from a CDR reader. */
  public static TaggedProfile readFrom(CdrReader reader, IorLimits limits) {
    long tag = reader.readUnsignedLong();
    byte[] data = reader.readOctetSequence();
    return new TaggedProfile(tag, data, limits);
  }

  /** Returns the unsigned profile tag value. */
  public long tag() {
    return tag;
  }

  /** Returns a defensive copy of the encoded profile data. */
  public byte[] profileData() {
    return Arrays.copyOf(profileData, profileData.length);
  }

  /** Returns the decoded IIOP profile body when this profile uses TAG_INTERNET_IOP. */
  public Optional<IiopProfile> internetIopProfile() {
    if (tag != IorTags.TAG_INTERNET_IOP) {
      return Optional.empty();
    }
    return Optional.of(IiopProfile.fromProfileData(profileData));
  }

  /** Writes this tagged profile to a CDR writer. */
  public void writeTo(CdrWriter writer) {
    writer.writeUnsignedLong(tag).writeOctetSequence(profileData);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof TaggedProfile that)) {
      return false;
    }
    return tag == that.tag && Arrays.equals(profileData, that.profileData);
  }

  @Override
  public int hashCode() {
    return 31 * Long.hashCode(tag) + Arrays.hashCode(profileData);
  }

  @Override
  public String toString() {
    return "TaggedProfile[tag=" + tag + ", dataLength=" + profileData.length + "]";
  }
}
