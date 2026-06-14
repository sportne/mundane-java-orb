package io.github.mundanej.mjo.notification;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Local in-JVM Notification Service entrypoint. */
public final class LocalNotificationService implements AutoCloseable {

  private final NotificationServiceOptions options;
  private final NotificationEventCompatibility eventCompatibility;
  private final Map<Long, LocalNotificationChannel> channels = new LinkedHashMap<>();
  private long nextChannelId = 1L;
  private boolean closed;

  private LocalNotificationService(
      NotificationServiceOptions options, NotificationEventCompatibility eventCompatibility) {
    this.options = Objects.requireNonNull(options, "options");
    this.eventCompatibility = Objects.requireNonNull(eventCompatibility, "eventCompatibility");
  }

  /** Creates a local Notification Service with default bounded options. */
  public static LocalNotificationService create() {
    return create(NotificationServiceOptions.defaults());
  }

  /** Creates a local Notification Service with explicit bounded options. */
  public static LocalNotificationService create(NotificationServiceOptions options) {
    return new LocalNotificationService(options, NotificationEventCompatibility.localBoundary());
  }

  /** Returns this service's immutable options. */
  public NotificationServiceOptions options() {
    return options;
  }

  /** Returns the local Event Service compatibility boundary identity. */
  public NotificationEventCompatibility eventCompatibility() {
    return eventCompatibility;
  }

  /** Creates a new local notification channel. */
  public synchronized LocalNotificationChannel createChannel() {
    requireOpen();
    if (channels.size() >= options.maxChannels()) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.CHANNEL_LIMIT_EXCEEDED,
          "maximum local Notification Service channel count reached");
    }
    LocalNotificationChannel channel = new LocalNotificationChannel(this, nextChannelId++);
    channels.put(Long.valueOf(channel.id()), channel);
    return channel;
  }

  /** Returns the number of currently active local channels. */
  public synchronized int activeChannelCount() {
    return channels.size();
  }

  synchronized void destroyChannel(LocalNotificationChannel channel) {
    requireOpen();
    Objects.requireNonNull(channel, "channel");
    if (channels.remove(Long.valueOf(channel.id())) == null) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.CHANNEL_DESTROYED,
          "notification channel is already destroyed: " + channel.id());
    }
  }

  synchronized void requireOpen() {
    if (closed) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.SERVICE_SHUTDOWN, "Notification Service is shut down");
    }
  }

  @Override
  public synchronized void close() {
    closed = true;
    channels.clear();
  }
}
