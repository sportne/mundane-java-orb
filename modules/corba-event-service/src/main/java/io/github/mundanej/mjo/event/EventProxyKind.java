package io.github.mundanej.mjo.event;

/** Local Event Service proxy handle kind. */
public enum EventProxyKind {
  /** Proxy that accepts pushed events from a push supplier. */
  PUSH_CONSUMER,

  /** Proxy that exposes events to a pull supplier. */
  PULL_CONSUMER,

  /** Proxy that pushes events to a push consumer. */
  PUSH_SUPPLIER,

  /** Proxy that supplies events to a pull consumer. */
  PULL_SUPPLIER
}
