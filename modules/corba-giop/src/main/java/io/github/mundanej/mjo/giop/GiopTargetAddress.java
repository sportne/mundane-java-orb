package io.github.mundanej.mjo.giop;

import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.TaggedProfile;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/** GIOP 1.2 TargetAddress value for request and locate messages. */
public final class GiopTargetAddress {

  /** KeyAddr discriminator. */
  public static final short KEY_ADDR = 0;

  /** ProfileAddr discriminator. */
  public static final short PROFILE_ADDR = 1;

  /** ReferenceAddr discriminator. */
  public static final short REFERENCE_ADDR = 2;

  private final short discriminator;
  private final byte[] objectKey;
  private final Optional<TaggedProfile> profile;
  private final long selectedProfileIndex;
  private final Optional<Ior> ior;

  private GiopTargetAddress(
      short discriminator,
      byte[] objectKey,
      Optional<TaggedProfile> profile,
      long selectedProfileIndex,
      Optional<Ior> ior) {
    this.discriminator = discriminator;
    this.objectKey = GiopModel.copyBytes(objectKey, "objectKey");
    this.profile = Objects.requireNonNull(profile, "profile");
    GiopModel.requireUnsignedLong(selectedProfileIndex, "selectedProfileIndex");
    this.selectedProfileIndex = selectedProfileIndex;
    this.ior = Objects.requireNonNull(ior, "ior");
  }

  /** Creates a KeyAddr target. */
  public static GiopTargetAddress keyAddr(byte[] objectKey) {
    return new GiopTargetAddress(KEY_ADDR, objectKey, Optional.empty(), 0, Optional.empty());
  }

  /** Creates a ProfileAddr target. */
  public static GiopTargetAddress profileAddr(TaggedProfile profile) {
    return new GiopTargetAddress(
        PROFILE_ADDR, new byte[0], Optional.of(profile), 0, Optional.empty());
  }

  /** Creates a ReferenceAddr target. */
  public static GiopTargetAddress referenceAddr(long selectedProfileIndex, Ior ior) {
    return new GiopTargetAddress(
        REFERENCE_ADDR,
        new byte[0],
        Optional.empty(),
        selectedProfileIndex,
        Optional.of(Objects.requireNonNull(ior, "ior")));
  }

  /** Returns the target-address discriminator. */
  public short discriminator() {
    return discriminator;
  }

  /** Returns true for KeyAddr targets. */
  public boolean isKeyAddr() {
    return discriminator == KEY_ADDR;
  }

  /** Returns the KeyAddr object key. */
  public byte[] objectKey() {
    if (!isKeyAddr()) {
      throw new GiopException(
          GiopDiagnosticCodes.INVALID_BODY, "target address does not carry a KeyAddr object key");
    }
    return Arrays.copyOf(objectKey, objectKey.length);
  }

  /** Returns the ProfileAddr tagged profile. */
  public TaggedProfile profile() {
    return profile.orElseThrow(
        () -> new GiopException(GiopDiagnosticCodes.INVALID_BODY, "target is not ProfileAddr"));
  }

  /** Returns the ReferenceAddr selected profile index. */
  public long selectedProfileIndex() {
    return selectedProfileIndex;
  }

  /** Returns the ReferenceAddr IOR. */
  public Ior ior() {
    return ior.orElseThrow(
        () -> new GiopException(GiopDiagnosticCodes.INVALID_BODY, "target is not ReferenceAddr"));
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof GiopTargetAddress that)) {
      return false;
    }
    return discriminator == that.discriminator
        && Arrays.equals(objectKey, that.objectKey)
        && profile.equals(that.profile)
        && selectedProfileIndex == that.selectedProfileIndex
        && ior.equals(that.ior);
  }

  @Override
  public int hashCode() {
    return Objects.hash(discriminator, profile, selectedProfileIndex, ior) * 31
        + Arrays.hashCode(objectKey);
  }

  @Override
  public String toString() {
    return switch (discriminator) {
      case KEY_ADDR -> "GiopTargetAddress[KeyAddr octets=" + objectKey.length + "]";
      case PROFILE_ADDR -> "GiopTargetAddress[ProfileAddr]";
      case REFERENCE_ADDR -> "GiopTargetAddress[ReferenceAddr index=" + selectedProfileIndex + "]";
      default -> "GiopTargetAddress[unknown]";
    };
  }
}
