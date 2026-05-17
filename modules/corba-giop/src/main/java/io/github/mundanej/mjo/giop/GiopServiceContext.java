package io.github.mundanej.mjo.giop;

import java.util.Arrays;
import java.util.Objects;

/** Opaque GIOP service context. */
public final class GiopServiceContext {

  private final long contextId;
  private final byte[] contextData;

  /** Creates a service context with defensively copied context data. */
  public GiopServiceContext(long contextId, byte[] contextData) {
    GiopModel.requireUnsignedLong(contextId, "contextId");
    this.contextId = contextId;
    this.contextData =
        Arrays.copyOf(Objects.requireNonNull(contextData, "contextData"), contextData.length);
  }

  /** Returns the unsigned service context identifier. */
  public long contextId() {
    return contextId;
  }

  /** Returns a defensive copy of the opaque context data. */
  public byte[] contextData() {
    return Arrays.copyOf(contextData, contextData.length);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof GiopServiceContext serviceContext
        && contextId == serviceContext.contextId
        && Arrays.equals(contextData, serviceContext.contextData);
  }

  @Override
  public int hashCode() {
    return 31 * Long.hashCode(contextId) + Arrays.hashCode(contextData);
  }

  @Override
  public String toString() {
    return "GiopServiceContext[contextId="
        + contextId
        + ", contextDataLength="
        + contextData.length
        + "]";
  }
}
