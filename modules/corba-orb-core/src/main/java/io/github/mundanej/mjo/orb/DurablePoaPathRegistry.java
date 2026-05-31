package io.github.mundanej.mjo.orb;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Registry of caller-approved persistent POA paths for durable-key rehydration. */
public final class DurablePoaPathRegistry {

  private final OrbIdentity identity;
  private final Set<List<String>> registeredPaths = new LinkedHashSet<>();
  private boolean closed;

  DurablePoaPathRegistry(OrbIdentity identity) {
    this.identity = LocalExceptionMapper.requireNonNull(identity, "identity");
  }

  /** Registers one durable POA path for later durable-key lookup. */
  public synchronized void register(List<String> poaPath) {
    requireOpen();
    List<String> checkedPath = validatePath(poaPath);
    if (!registeredPaths.add(checkedPath)) {
      throw LocalExceptionMapper.badParam(
          "Durable POA path is already registered: " + pathString(checkedPath));
    }
  }

  /** Unregisters one durable POA path. */
  public synchronized void unregister(List<String> poaPath) {
    requireOpen();
    List<String> checkedPath = validatePath(poaPath);
    if (!registeredPaths.remove(checkedPath)) {
      throw LocalExceptionMapper.objectNotExist(
          "Durable POA path is not registered: " + pathString(checkedPath));
    }
  }

  /** Returns whether a durable POA path is currently registered. */
  public synchronized boolean contains(List<String> poaPath) {
    requireOpen();
    return registeredPaths.contains(validatePath(poaPath));
  }

  /** Requires that a decoded durable key targets this ORB and a registered POA path. */
  public synchronized void requireRegistered(DurableObjectKey key) {
    requireOpen();
    DurableObjectKey checkedKey = LocalExceptionMapper.requireNonNull(key, "key");
    requireDurableOrb();
    if (!identity.requireDurableOrbId().equals(checkedKey.orbId())) {
      throw LocalExceptionMapper.objectNotExist("Durable POA path belongs to a different ORB");
    }
    if (!registeredPaths.contains(checkedKey.poaPath())) {
      throw LocalExceptionMapper.objectNotExist(
          "Durable POA path is not registered: " + checkedKey.poaPathString());
    }
  }

  synchronized void close() {
    closed = true;
    registeredPaths.clear();
  }

  private List<String> validatePath(List<String> poaPath) {
    requireDurableOrb();
    if (poaPath == null) {
      throw LocalExceptionMapper.badParam("Invalid durable POA path: poaPath must not be null");
    }
    for (String component : poaPath) {
      if (component == null) {
        throw LocalExceptionMapper.badParam(
            "Invalid durable POA path: POA path component must not be null");
      }
    }
    try {
      DurableObjectKey key =
          new DurableObjectKey(identity.requireDurableOrbId(), poaPath, new byte[] {1}, 0);
      return key.poaPath();
    } catch (IllegalArgumentException exception) {
      throw LocalExceptionMapper.badParam("Invalid durable POA path: " + exception.getMessage());
    }
  }

  private void requireDurableOrb() {
    if (!identity.durable()) {
      throw LocalExceptionMapper.badParam(
          "Durable POA path registry requires a durable ORB identity");
    }
  }

  private void requireOpen() {
    if (closed) {
      throw LocalExceptionMapper.badInvOrder("Durable POA path registry is shut down");
    }
  }

  private static String pathString(List<String> path) {
    return "/" + String.join("/", path);
  }
}
