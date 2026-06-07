package io.github.mundanej.mjo.event;

import java.util.Objects;

/** Base class for local Event Service proxy handles. */
public abstract class LocalEventProxy {

  private final LocalEventChannel channel;
  private final long id;
  private final EventProxyKind kind;
  private boolean destroyed;

  LocalEventProxy(LocalEventChannel channel, long id, EventProxyKind kind) {
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

  /** Returns this proxy's Event Service role. */
  public final EventProxyKind kind() {
    return kind;
  }

  /** Destroys this proxy handle. */
  public final void destroy() {
    requireAlive();
    onDestroy();
    destroyed = true;
  }

  /** Returns whether this proxy handle has been destroyed. */
  public final boolean isDestroyed() {
    return destroyed;
  }

  /** Checks that this proxy and its owning channel are usable. */
  public final void requireAlive() {
    channel.requireAlive();
    if (destroyed) {
      throw new EventServiceException(
          EventServiceDiagnosticCodes.PROXY_DESTROYED, "event proxy is destroyed: " + id);
    }
  }

  /** Hook for concrete proxies to disconnect local callbacks before destruction. */
  void onDestroy() {
    // Default proxy handles have no callback state.
  }
}
