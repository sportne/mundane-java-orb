package io.github.mundanej.mjo.notification;

/** Caller-provided local Notification Service resource limits. */
public record NotificationServiceOptions(
    int maxChannels, int maxSuppliersPerChannel, int maxConsumersPerChannel) {

  /** Default maximum number of channels owned by one local service. */
  public static final int DEFAULT_MAX_CHANNELS = 16;

  /** Default maximum supplier-side proxies owned by one channel. */
  public static final int DEFAULT_MAX_SUPPLIERS_PER_CHANNEL = 64;

  /** Default maximum consumer-side proxies owned by one channel. */
  public static final int DEFAULT_MAX_CONSUMERS_PER_CHANNEL = 64;

  private static final int MAX_SUPPORTED_LIMIT = 65_535;

  /** Creates validated Notification Service options. */
  public NotificationServiceOptions {
    requireLimit(maxChannels, "maxChannels");
    requireLimit(maxSuppliersPerChannel, "maxSuppliersPerChannel");
    requireLimit(maxConsumersPerChannel, "maxConsumersPerChannel");
  }

  /** Returns default bounded local Notification Service options. */
  public static NotificationServiceOptions defaults() {
    return new NotificationServiceOptions(
        DEFAULT_MAX_CHANNELS, DEFAULT_MAX_SUPPLIERS_PER_CHANNEL, DEFAULT_MAX_CONSUMERS_PER_CHANNEL);
  }

  private static void requireLimit(int value, String name) {
    if (value < 1 || value > MAX_SUPPORTED_LIMIT) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.INVALID_LIMIT,
          name + " must be between 1 and " + MAX_SUPPORTED_LIMIT);
    }
  }
}
