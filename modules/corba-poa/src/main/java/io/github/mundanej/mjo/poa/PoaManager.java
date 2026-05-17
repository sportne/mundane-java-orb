package io.github.mundanej.mjo.poa;

/** Local POA manager state machine for request dispatch decisions. */
public final class PoaManager {

  private State state = State.ACTIVE;

  /** Marks this manager active and releases held requests. */
  public synchronized void activate() {
    ensureNotInactive();
    state = State.ACTIVE;
    notifyAll();
  }

  /** Holds future requests until the manager becomes active or terminal. */
  public synchronized void holdRequests() {
    ensureNotInactive();
    state = State.HOLDING;
  }

  /** Rejects future requests without destroying the POA. */
  public synchronized void discardRequests() {
    ensureNotInactive();
    state = State.DISCARDING;
    notifyAll();
  }

  /** Marks the manager inactive and rejects future requests. */
  public synchronized void deactivate() {
    state = State.INACTIVE;
    notifyAll();
  }

  /** Returns the current manager state. */
  public synchronized State state() {
    return state;
  }

  void awaitDispatchPermission() {
    synchronized (this) {
      while (state == State.HOLDING) {
        try {
          wait();
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          throw PoaExceptions.badInvOrder("Interrupted while POA manager was holding requests");
        }
      }
      if (state == State.DISCARDING) {
        throw PoaExceptions.badInvOrder("POA manager is discarding requests");
      }
      if (state == State.INACTIVE) {
        throw PoaExceptions.badInvOrder("POA manager is inactive");
      }
    }
  }

  private void ensureNotInactive() {
    if (state == State.INACTIVE) {
      throw PoaExceptions.badInvOrder("POA manager is inactive");
    }
  }

  /** Local request handling state. */
  public enum State {
    ACTIVE,
    HOLDING,
    DISCARDING,
    INACTIVE
  }
}
