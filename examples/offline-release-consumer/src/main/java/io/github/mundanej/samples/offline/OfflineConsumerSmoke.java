package io.github.mundanej.samples.offline;

import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.ior.CorbalocAddress;
import io.github.mundanej.mjo.ior.CorbalocUrl;

/** Minimal downstream consumer compiled from staged mundane Java ORB artifacts. */
public final class OfflineConsumerSmoke {

  private OfflineConsumerSmoke() {}

  /** Runs representative API calls from BOM-aligned artifacts. */
  public static void main(String[] args) {
    BoundedLimit limit = new BoundedLimit("sample", 32);
    if (!limit.accepts(16)) {
      throw new IllegalStateException("BoundedLimit did not accept an in-range value");
    }

    CorbalocUrl url = CorbalocUrl.parse("corbaloc:rir:/NameService");
    if (url.addresses().getFirst().kind() != CorbalocAddress.Kind.RIR) {
      throw new IllegalStateException("corbaloc:rir address did not parse");
    }
    if (!"NameService".equals(url.keyString())) {
      throw new IllegalStateException("corbaloc key did not round trip");
    }
  }
}
