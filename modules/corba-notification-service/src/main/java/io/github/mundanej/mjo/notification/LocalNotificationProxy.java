package io.github.mundanej.mjo.notification;

import io.github.mundanej.mjo.event.EventProxyKind;
import java.util.Objects;

/** Base class for local Notification Service proxy handles. */
public abstract class LocalNotificationProxy {

  private final LocalNotificationChannel channel;
  private final long id;
  private final NotificationProxyKind kind;
  private boolean destroyed;

  LocalNotificationProxy(LocalNotificationChannel channel, long id, NotificationProxyKind kind) {
    this.channel = Objects.requireNonNull(channel, "channel");
    this.id = id;
    this.kind = Objects.requireNonNull(kind, "kind");
  }

  /** Returns this proxy's local channel-scoped id. */
  public final long id() {
    return id;
  }

  /** Returns this proxy's owning local channel id. */
  public final long channelId() {
    return channel.id();
  }

  /** Returns this proxy's Notification Service role. */
  public final NotificationProxyKind kind() {
    return kind;
  }

  /** Returns this proxy's compatible Event Service role. */
  public final EventProxyKind eventProxyKind() {
    return kind.eventProxyKind();
  }

  /** Destroys this proxy handle. */
  public final void destroy() {
    requireAlive();
    destroyed = true;
    onDestroy();
    channel.removeProxy(this);
  }

  /** Returns whether this proxy handle has been destroyed. */
  public final boolean isDestroyed() {
    return destroyed;
  }

  /** Checks that this proxy and its owning channel are usable. */
  public final void requireAlive() {
    channel.requireAlive();
    if (destroyed) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.PROXY_DESTROYED,
          "notification proxy is destroyed: " + id);
    }
  }

  void onDestroy() {}
}
