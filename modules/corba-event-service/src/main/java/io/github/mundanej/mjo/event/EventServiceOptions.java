package io.github.mundanej.mjo.event;

/** Caller-provided local Event Service resource limits. */
public record EventServiceOptions(
    int maxChannels, int maxSuppliersPerChannel, int maxConsumersPerChannel, int maxPendingEvents) {

  /** Default maximum number of channels owned by one local service. */
  public static final int DEFAULT_MAX_CHANNELS = 16;

  /** Default maximum supplier-side proxies owned by one channel. */
  public static final int DEFAULT_MAX_SUPPLIERS_PER_CHANNEL = 64;

  /** Default maximum consumer-side proxies owned by one channel. */
  public static final int DEFAULT_MAX_CONSUMERS_PER_CHANNEL = 64;

  /** Default maximum queued events used by later delivery slices. */
  public static final int DEFAULT_MAX_PENDING_EVENTS = 256;

  private static final int MAX_SUPPORTED_LIMIT = 65_535;

  /** Creates validated Event Service options. */
  public EventServiceOptions {
    requireLimit(maxChannels, "maxChannels");
    requireLimit(maxSuppliersPerChannel, "maxSuppliersPerChannel");
    requireLimit(maxConsumersPerChannel, "maxConsumersPerChannel");
    requireLimit(maxPendingEvents, "maxPendingEvents");
  }

  /** Returns default bounded local Event Service options. */
  public static EventServiceOptions defaults() {
    return new EventServiceOptions(
        DEFAULT_MAX_CHANNELS,
        DEFAULT_MAX_SUPPLIERS_PER_CHANNEL,
        DEFAULT_MAX_CONSUMERS_PER_CHANNEL,
        DEFAULT_MAX_PENDING_EVENTS);
  }

  private static void requireLimit(int value, String name) {
    if (value < 1 || value > MAX_SUPPORTED_LIMIT) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.INVALID_LIMIT,
          name + " must be between 1 and " + MAX_SUPPORTED_LIMIT);
    }
  }
}
