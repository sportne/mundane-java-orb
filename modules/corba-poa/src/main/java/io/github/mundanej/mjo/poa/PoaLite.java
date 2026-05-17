package io.github.mundanej.mjo.poa;

import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal POA-lite runtime for generated-style local servant dispatch. */
public final class PoaLite {

  private final LocalOrb orb;
  private final PoaLitePolicySet policySet;
  private final Map<String, ActiveServant<?>> activeObjectMap = new LinkedHashMap<>();
  private final IdentityHashMap<Object, String> objectIdsByServant = new IdentityHashMap<>();
  private boolean shutdown;

  private PoaLite(LocalOrb orb, PoaLitePolicySet policySet) {
    this.orb = PoaLiteExceptions.requireNonNull(orb, "orb");
    this.policySet = PoaLiteExceptions.requireNonNull(policySet, "policySet");
  }

  /** Creates an active RootPOA-lite with the approved fixed policy profile. */
  public static PoaLite createRoot(LocalOrb orb) {
    return createRoot(orb, PoaLitePolicySet.approvedProfile());
  }

  /** Creates an active RootPOA-lite with an explicitly supplied approved policy profile. */
  public static PoaLite createRoot(LocalOrb orb, PoaLitePolicySet policySet) {
    return new PoaLite(orb, policySet);
  }

  /** Returns the fixed POA-lite policy profile. */
  public PoaLitePolicySet policySet() {
    return policySet;
  }

  /** Activates a servant and returns its local object reference. */
  public synchronized <T, S> LocalObjectReference<T> activateServant(
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      S servant,
      PoaServantDispatcher<? super S> dispatcher) {
    requireActive();
    PoaLiteExceptions.requireNonNull(javaType, "javaType");
    PoaLiteExceptions.requireNonNull(descriptor, "descriptor");
    PoaLiteExceptions.requireNonNull(servant, "servant");
    PoaLiteExceptions.requireNonNull(dispatcher, "dispatcher");
    if (objectIdsByServant.containsKey(servant)) {
      throw PoaLiteExceptions.badParam(
          "POA-lite UNIQUE_ID policy rejects duplicate servant activation");
    }
    String[] assignedObjectId = new String[1];
    LocalObjectReference<T> reference =
        orb.bind(javaType, descriptor, request -> dispatch(assignedObjectId[0], request));
    String objectId = reference.objectId();
    assignedObjectId[0] = objectId;
    activeObjectMap.put(objectId, new ActiveServant<>(servant, dispatcher));
    objectIdsByServant.put(servant, objectId);
    return reference;
  }

  /** Deactivates one active object id. */
  public synchronized void deactivateObject(String objectId) {
    requireActive();
    String checkedObjectId = PoaLiteExceptions.requireNonBlank(objectId, "objectId");
    ActiveServant<?> removed = activeObjectMap.remove(checkedObjectId);
    if (removed == null) {
      throw PoaLiteExceptions.objectNotExist("Unknown POA-lite object id: " + checkedObjectId);
    }
    objectIdsByServant.remove(removed.servant());
  }

  /** Shuts this POA-lite down. Repeated shutdown calls are safe. */
  public synchronized void shutdown() {
    shutdown = true;
    activeObjectMap.clear();
    objectIdsByServant.clear();
  }

  /** Returns whether this POA-lite has been shut down. */
  public synchronized boolean isShutdown() {
    return shutdown;
  }

  /** Returns the number of retained active object map entries. */
  public synchronized int activeObjectCount() {
    return activeObjectMap.size();
  }

  private synchronized Object dispatch(String objectId, LocalInvocationRequest request)
      throws Exception {
    requireActive();
    ActiveServant<?> activeServant = activeObjectMap.get(objectId);
    if (activeServant == null) {
      throw PoaLiteExceptions.objectNotExist("Unknown POA-lite object id: " + objectId);
    }
    return activeServant.invoke(request);
  }

  private void requireActive() {
    if (shutdown) {
      throw PoaLiteExceptions.badInvOrder("POA-lite is shut down");
    }
  }

  private record ActiveServant<S>(S servant, PoaServantDispatcher<? super S> dispatcher) {

    private Object invoke(LocalInvocationRequest request) throws Exception {
      return dispatcher.invoke(servant, request);
    }
  }
}
