package io.github.mundanej.mjo.orb;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/** Explicit ORB identity used to separate transient and durable object references. */
public final class OrbIdentity {

  private static final int MAX_ORB_ID_OCTETS = 128;
  private static final OrbIdentity TRANSIENT_LOCAL = new OrbIdentity(null);

  private final String orbId;

  private OrbIdentity(String orbId) {
    this.orbId = orbId;
  }

  /** Returns the default process-local transient ORB identity. */
  public static OrbIdentity transientLocal() {
    return TRANSIENT_LOCAL;
  }

  /** Returns a durable ORB identity with a caller-configured stable identifier. */
  public static OrbIdentity durable(String orbId) {
    return new OrbIdentity(requireIdentifier(orbId, "orbId", MAX_ORB_ID_OCTETS));
  }

  /** Returns true when this identity can be used for durable references. */
  public boolean durable() {
    return orbId != null;
  }

  /** Returns the durable ORB id when this identity is durable. */
  public Optional<String> durableOrbId() {
    return Optional.ofNullable(orbId);
  }

  /** Returns the durable ORB id or throws when this identity is transient. */
  public String requireDurableOrbId() {
    if (orbId == null) {
      throw new IllegalStateException("transient ORB identity cannot create durable references");
    }
    return orbId;
  }

  static String requireIdentifier(String value, String label, int maxOctets) {
    Objects.requireNonNull(value, label);
    if (value.isBlank()) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
    byte[] octets = value.getBytes(StandardCharsets.US_ASCII);
    if (!StandardCharsets.US_ASCII.newEncoder().canEncode(value)) {
      throw new IllegalArgumentException(label + " must be US-ASCII");
    }
    if (octets.length > maxOctets) {
      throw new IllegalArgumentException(label + " exceeds " + maxOctets + " octets");
    }
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      boolean allowed =
          (current >= 'a' && current <= 'z')
              || (current >= 'A' && current <= 'Z')
              || (current >= '0' && current <= '9')
              || current == '.'
              || current == '_'
              || current == '-'
              || current == ':';
      if (!allowed) {
        throw new IllegalArgumentException(label + " contains unsupported character: " + current);
      }
    }
    return value;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof OrbIdentity that && Objects.equals(orbId, that.orbId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(orbId);
  }

  @Override
  public String toString() {
    return durable() ? "OrbIdentity[orbId=" + orbId + "]" : "OrbIdentity[transientLocal]";
  }
}
