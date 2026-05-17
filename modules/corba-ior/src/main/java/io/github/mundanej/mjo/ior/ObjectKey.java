package io.github.mundanej.mjo.ior;

import java.util.Arrays;

/** Immutable opaque object key carried by IIOP profiles and object URLs. */
public final class ObjectKey {

  private final byte[] octets;

  /** Creates an object key with the default IOR bounds. */
  public ObjectKey(byte[] octets) {
    this(octets, IorLimits.defaults());
  }

  /** Creates an object key with caller-supplied bounds. */
  public ObjectKey(byte[] octets, IorLimits limits) {
    this.octets = IorWire.copyLimited(octets, limits, "octets");
  }

  /** Returns an empty object key. */
  public static ObjectKey empty() {
    return new ObjectKey(new byte[0]);
  }

  /** Returns a defensive copy of the object-key octets. */
  public byte[] octets() {
    return Arrays.copyOf(octets, octets.length);
  }

  /** Returns a canonical uppercase hexadecimal representation of the key octets. */
  public String toHex() {
    return IorWire.encodeHex(octets);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ObjectKey that && Arrays.equals(octets, that.octets);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(octets);
  }

  @Override
  public String toString() {
    return "ObjectKey[octets=" + octets.length + "]";
  }
}
