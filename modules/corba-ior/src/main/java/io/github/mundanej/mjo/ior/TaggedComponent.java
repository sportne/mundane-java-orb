package io.github.mundanej.mjo.ior;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import java.util.Arrays;
import java.util.Objects;

/** Immutable IOP tagged component. */
public final class TaggedComponent {

  private final long tag;
  private final byte[] componentData;

  /** Creates a tagged component with default bounds. */
  public TaggedComponent(long tag, byte[] componentData) {
    this(tag, componentData, IorLimits.defaults());
  }

  /** Creates a tagged component with caller-supplied bounds. */
  public TaggedComponent(long tag, byte[] componentData, IorLimits limits) {
    Objects.requireNonNull(componentData, "componentData");
    this.tag = IorWire.requireUnsignedLong(tag, "component tag");
    limits.requireWithin(limits.componentDataOctets(), componentData.length);
    this.componentData = Arrays.copyOf(componentData, componentData.length);
  }

  /** Reads one tagged component from a CDR reader. */
  public static TaggedComponent readFrom(CdrReader reader, IorLimits limits) {
    long tag = reader.readUnsignedLong();
    byte[] data = reader.readOctetSequence();
    return new TaggedComponent(tag, data, limits);
  }

  /** Returns the unsigned component tag value. */
  public long tag() {
    return tag;
  }

  /** Returns a defensive copy of the encoded component data. */
  public byte[] componentData() {
    return Arrays.copyOf(componentData, componentData.length);
  }

  /** Writes this tagged component to a CDR writer. */
  public void writeTo(CdrWriter writer) {
    writer.writeUnsignedLong(tag).writeOctetSequence(componentData);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof TaggedComponent that)) {
      return false;
    }
    return tag == that.tag && Arrays.equals(componentData, that.componentData);
  }

  @Override
  public int hashCode() {
    return 31 * Long.hashCode(tag) + Arrays.hashCode(componentData);
  }

  @Override
  public String toString() {
    return "TaggedComponent[tag=" + tag + ", dataLength=" + componentData.length + "]";
  }
}
