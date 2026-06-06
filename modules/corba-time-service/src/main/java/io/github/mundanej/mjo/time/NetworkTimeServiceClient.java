package io.github.mundanej.mjo.time;

import io.github.mundanej.mjo.iiop.IiopObjectReference;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.iiop.IiopOrbClient;
import io.github.mundanej.mjo.ior.Ior;
import java.util.List;
import java.util.Objects;

/** Client helper for invoking the supported Time Service IIOP operations. */
public final class NetworkTimeServiceClient implements AutoCloseable {

  private final IiopOrbClient client;

  private NetworkTimeServiceClient(IiopOrbClient client) {
    this.client = Objects.requireNonNull(client, "client");
  }

  /** Connects to a Time Service IOR. */
  public static NetworkTimeServiceClient connect(Ior ior, IiopOptions options) {
    return connect(IiopObjectReference.fromIor(ior), options);
  }

  /** Connects to a Time Service IIOP object reference. */
  public static NetworkTimeServiceClient connect(
      IiopObjectReference reference, IiopOptions options) {
    return new NetworkTimeServiceClient(IiopOrbClient.connect(reference, options));
  }

  /** Invokes TimeService::universal_time. */
  public UtcTime universalTime() {
    return (UtcTime)
        client.invoke(
            TimeServiceDescriptors.UNIVERSAL_TIME, TimeServiceIiopCodec.INSTANCE, List.of());
  }

  /** Invokes TimeService::new_universal_time. */
  public UtcTime newUniversalTime(long timeTicks, long inaccuracyTicks, short tdfMinutes) {
    UtcTime validated = new UtcTime(timeTicks, inaccuracyTicks, tdfMinutes);
    return (UtcTime)
        client.invoke(
            TimeServiceDescriptors.NEW_UNIVERSAL_TIME,
            TimeServiceIiopCodec.INSTANCE,
            List.of(
                validated.timeTicks(),
                TimeServiceDescriptors.inacclo(validated),
                TimeServiceDescriptors.inacchi(validated),
                validated.tdfMinutes()));
  }

  /** Invokes TimeService::new_interval. */
  public TimeInterval newInterval(long lowerTicks, long upperTicks) {
    TimeInterval validated = new TimeInterval(lowerTicks, upperTicks);
    return (TimeInterval)
        client.invoke(
            TimeServiceDescriptors.NEW_INTERVAL,
            TimeServiceIiopCodec.INSTANCE,
            List.of(validated.lowerBoundTicks(), validated.upperBoundTicks()));
  }

  @Override
  public void close() {
    client.close();
  }
}
