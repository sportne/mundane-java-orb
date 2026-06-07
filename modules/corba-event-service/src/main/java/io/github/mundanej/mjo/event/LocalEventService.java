package io.github.mundanej.mjo.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Local in-JVM Event Service entrypoint. */
public final class LocalEventService implements AutoCloseable {

  private final EventServiceOptions options;
  private final Map<Long, LocalEventChannel> channels = new LinkedHashMap<>();
  private long nextChannelId = 1L;
  private boolean closed;

  private LocalEventService(EventServiceOptions options) {
    this.options = Objects.requireNonNull(options, "options");
  }

  /** Creates a local Event Service with default bounded options. */
  public static LocalEventService create() {
    return create(EventServiceOptions.defaults());
  }

  /** Creates a local Event Service with explicit bounded options. */
  public static LocalEventService create(EventServiceOptions options) {
    return new LocalEventService(options);
  }

  /** Returns this service's immutable options. */
  public EventServiceOptions options() {
    return options;
  }

  /** Creates a new local event channel. */
  public synchronized LocalEventChannel createChannel() {
    requireOpen();
    if (channels.size() >= options.maxChannels()) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.CHANNEL_LIMIT_EXCEEDED,
          "maximum local Event Service channel count reached");
    }
    LocalEventChannel channel = new LocalEventChannel(this, nextChannelId++);
    channels.put(Long.valueOf(channel.id()), channel);
    return channel;
  }

  /** Returns the number of currently active local channels. */
  public synchronized int activeChannelCount() {
    return channels.size();
  }

  synchronized void destroyChannel(LocalEventChannel channel) {
    requireOpen();
    Objects.requireNonNull(channel, "channel");
    if (channels.remove(Long.valueOf(channel.id())) == null) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.CHANNEL_DESTROYED,
          "event channel is already destroyed: " + channel.id());
    }
  }

  synchronized void requireOpen() {
    if (closed) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.SERVICE_SHUTDOWN, "Event Service is shut down");
    }
  }

  @Override
  public synchronized void close() {
    closed = true;
    channels.clear();
  }
}
