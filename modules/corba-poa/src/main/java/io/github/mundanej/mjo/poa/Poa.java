package io.github.mundanej.mjo.poa;

import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Local Portable Object Adapter runtime for generated-style servant dispatch. */
public final class Poa {

  private final LocalOrb orb;
  private final Poa parent;
  private final String name;
  private final String path;
  private final PoaPolicySet policySet;
  private final PoaManager manager;
  private final Map<String, ActiveServant<?>> activeObjectMap = new LinkedHashMap<>();
  private final IdentityHashMap<Object, List<String>> objectIdsByServant = new IdentityHashMap<>();
  private final Map<String, LocalObjectReference<?>> referencesByObjectId = new LinkedHashMap<>();
  private final Map<String, PoaServantDispatcher<Object>> dispatchersByObjectId =
      new LinkedHashMap<>();
  private final Map<String, Poa> children = new LinkedHashMap<>();
  private DefaultServant<?> defaultServant;
  private PoaServantActivator servantActivator;
  private PoaServantLocator servantLocator;
  private PoaAdapterActivator adapterActivator;
  private boolean destroyed;

  private Poa(
      LocalOrb orb,
      Poa parent,
      String name,
      String path,
      PoaPolicySet policySet,
      PoaManager manager) {
    this.orb = PoaExceptions.requireNonNull(orb, "orb");
    this.parent = parent;
    this.name = PoaExceptions.requireNonBlank(name, "name");
    this.path = PoaExceptions.requireNonBlank(path, "path");
    this.policySet = PoaExceptions.requireNonNull(policySet, "policySet");
    this.manager = PoaExceptions.requireNonNull(manager, "manager");
  }

  /** Creates an active RootPOA with the retained transient default profile. */
  public static Poa createRoot(LocalOrb orb) {
    return createRoot(orb, PoaPolicySet.transientRetainedProfile());
  }

  /** Creates an active RootPOA with an explicit validated policy set. */
  public static Poa createRoot(LocalOrb orb, PoaPolicySet policySet) {
    return new Poa(orb, null, "RootPOA", "/RootPOA", policySet, new PoaManager());
  }

  /** Returns this POA's simple adapter name. */
  public String name() {
    return name;
  }

  /** Returns this POA's stable local path. */
  public String path() {
    return path;
  }

  /** Returns this POA's policy set. */
  public PoaPolicySet policySet() {
    return policySet;
  }

  /** Returns the manager that controls local request dispatch. */
  public PoaManager manager() {
    return manager;
  }

  /** Creates or replaces the adapter activator used by explicit child lookup. */
  public synchronized void setAdapterActivator(PoaAdapterActivator adapterActivator) {
    requireNotDestroyed();
    this.adapterActivator = PoaExceptions.requireNonNull(adapterActivator, "adapterActivator");
  }

  /** Registers a retained servant activator for missing active entries. */
  public synchronized void setServantActivator(PoaServantActivator servantActivator) {
    requireNotDestroyed();
    if (policySet.servantRetentionPolicy() != PoaPolicySet.ServantRetentionPolicy.RETAIN
        || policySet.requestProcessingPolicy()
            != PoaPolicySet.RequestProcessingPolicy.USE_SERVANT_MANAGER) {
      throw PoaExceptions.badParam("ServantActivator requires RETAIN and USE_SERVANT_MANAGER");
    }
    this.servantActivator = PoaExceptions.requireNonNull(servantActivator, "servantActivator");
  }

  /** Registers a non-retained servant locator for per-request servant lookup. */
  public synchronized void setServantLocator(PoaServantLocator servantLocator) {
    requireNotDestroyed();
    if (policySet.servantRetentionPolicy() != PoaPolicySet.ServantRetentionPolicy.NON_RETAIN
        || policySet.requestProcessingPolicy()
            != PoaPolicySet.RequestProcessingPolicy.USE_SERVANT_MANAGER) {
      throw PoaExceptions.badParam("ServantLocator requires NON_RETAIN and USE_SERVANT_MANAGER");
    }
    this.servantLocator = PoaExceptions.requireNonNull(servantLocator, "servantLocator");
  }

  /** Registers the default servant used by USE_DEFAULT_SERVANT policies. */
  public synchronized <S> void setDefaultServant(
      S servant, PoaServantDispatcher<? super S> dispatcher) {
    requireNotDestroyed();
    if (policySet.requestProcessingPolicy()
        != PoaPolicySet.RequestProcessingPolicy.USE_DEFAULT_SERVANT) {
      throw PoaExceptions.badParam("Default servant requires USE_DEFAULT_SERVANT");
    }
    PoaExceptions.requireNonNull(servant, "servant");
    PoaExceptions.requireNonNull(dispatcher, "dispatcher");
    defaultServant = new DefaultServant<>(servant, dispatcher);
  }

  /** Creates a direct child POA with this POA's manager. */
  public synchronized Poa createChild(String childName, PoaPolicySet childPolicySet) {
    requireNotDestroyed();
    String checkedName = PoaExceptions.requireNonBlank(childName, "childName");
    if (children.containsKey(checkedName)) {
      throw PoaExceptions.badParam("Child POA already exists: " + checkedName);
    }
    Poa child =
        new Poa(
            orb,
            this,
            checkedName,
            path + "/" + checkedName,
            PoaExceptions.requireNonNull(childPolicySet, "childPolicySet"),
            manager);
    children.put(checkedName, child);
    return child;
  }

  /** Finds a direct child POA, optionally asking the adapter activator to create it. */
  public synchronized Poa findChild(String childName, boolean activate) {
    requireNotDestroyed();
    String checkedName = PoaExceptions.requireNonBlank(childName, "childName");
    Poa child = children.get(checkedName);
    if (child != null || !activate || adapterActivator == null) {
      return child;
    }
    Poa activated =
        PoaExceptions.requireNonNull(adapterActivator.createChild(this, checkedName), "child");
    if (!checkedName.equals(activated.name())) {
      throw PoaExceptions.badParam("AdapterActivator returned child with a different name");
    }
    if (activated.parent != this) {
      throw PoaExceptions.badParam("AdapterActivator returned child from a different parent");
    }
    children.putIfAbsent(checkedName, activated);
    return children.get(checkedName);
  }

  /** Activates a servant with a system-assigned object id. */
  public synchronized <T, S> LocalObjectReference<T> activateServant(
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      S servant,
      PoaServantDispatcher<? super S> dispatcher) {
    requireSystemId();
    return activateRetained(null, javaType, descriptor, servant, dispatcher);
  }

  /** Activates a servant with a user-assigned object id. */
  public synchronized <T, S> LocalObjectReference<T> activateServantWithId(
      String objectId,
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      S servant,
      PoaServantDispatcher<? super S> dispatcher) {
    requireUserId();
    return activateRetained(
        PoaExceptions.requireNonBlank(objectId, "objectId"),
        javaType,
        descriptor,
        servant,
        dispatcher);
  }

  /** Creates a reference for later default-servant or servant-manager dispatch. */
  public synchronized <T> LocalObjectReference<T> createReference(
      Class<T> javaType, IdlGeneratedTypeDescriptor descriptor) {
    requireSystemId();
    return bindReference(null, javaType, descriptor, null);
  }

  /** Creates a system-id reference with a dispatcher for servant-manager dispatch. */
  public synchronized <T> LocalObjectReference<T> createReference(
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      PoaServantDispatcher<Object> dispatcher) {
    requireSystemId();
    return bindReference(null, javaType, descriptor, dispatcher);
  }

  /** Creates a user-id reference for later default-servant or servant-manager dispatch. */
  public synchronized <T> LocalObjectReference<T> createReferenceWithId(
      String objectId, Class<T> javaType, IdlGeneratedTypeDescriptor descriptor) {
    requireUserId();
    return bindReference(
        PoaExceptions.requireNonBlank(objectId, "objectId"), javaType, descriptor, null);
  }

  /** Creates a user-id reference with a dispatcher for servant-manager dispatch. */
  public synchronized <T> LocalObjectReference<T> createReferenceWithId(
      String objectId,
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      PoaServantDispatcher<Object> dispatcher) {
    requireUserId();
    return bindReference(
        PoaExceptions.requireNonBlank(objectId, "objectId"), javaType, descriptor, dispatcher);
  }

  /** Returns or implicitly activates a reference for one servant. */
  public synchronized <T, S> LocalObjectReference<T> servantToReference(
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      S servant,
      PoaServantDispatcher<? super S> dispatcher) {
    requireNotDestroyed();
    requireManagerActiveForActivation();
    PoaExceptions.requireNonNull(servant, "servant");
    List<String> objectIds = objectIdsByServant.get(servant);
    if (objectIds != null && !objectIds.isEmpty()) {
      @SuppressWarnings("unchecked")
      LocalObjectReference<T> reference =
          (LocalObjectReference<T>) referencesByObjectId.get(objectIds.get(0));
      return reference;
    }
    if (policySet.implicitActivationPolicy()
        != PoaPolicySet.ImplicitActivationPolicy.IMPLICIT_ACTIVATION) {
      throw PoaExceptions.badInvOrder("Implicit activation is disabled for this POA");
    }
    return activateRetained(null, javaType, descriptor, servant, dispatcher);
  }

  /** Deactivates one retained active object. */
  public synchronized void deactivateObject(String objectId) {
    requireNotDestroyed();
    String checkedObjectId = PoaExceptions.requireNonBlank(objectId, "objectId");
    ActiveServant<?> removed = activeObjectMap.remove(checkedObjectId);
    if (removed == null) {
      throw PoaExceptions.objectNotExist("Unknown POA object id: " + checkedObjectId);
    }
    removeServantObjectId(removed.servant(), checkedObjectId);
    referencesByObjectId.remove(checkedObjectId);
    dispatchersByObjectId.remove(checkedObjectId);
    orb.unbind(checkedObjectId);
  }

  /** Destroys this local POA and releases retained local state. */
  public synchronized void destroy() {
    if (destroyed) {
      return;
    }
    destroyed = true;
    manager.deactivate();
    for (String objectId : new ArrayList<>(referencesByObjectId.keySet())) {
      orb.unbind(objectId);
    }
    referencesByObjectId.clear();
    dispatchersByObjectId.clear();
    activeObjectMap.clear();
    objectIdsByServant.clear();
    children.clear();
  }

  /** Returns whether this POA has been destroyed. */
  public synchronized boolean isDestroyed() {
    return destroyed;
  }

  /** Returns the number of retained active-object-map entries. */
  public synchronized int activeObjectCount() {
    return activeObjectMap.size();
  }

  private <T, S> LocalObjectReference<T> activateRetained(
      String requestedObjectId,
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      S servant,
      PoaServantDispatcher<? super S> dispatcher) {
    requireNotDestroyed();
    requireManagerActiveForActivation();
    if (policySet.servantRetentionPolicy() != PoaPolicySet.ServantRetentionPolicy.RETAIN) {
      throw PoaExceptions.badParam("Explicit activation requires RETAIN");
    }
    PoaExceptions.requireNonNull(servant, "servant");
    PoaExceptions.requireNonNull(dispatcher, "dispatcher");
    if (policySet.idUniquenessPolicy() == PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID
        && objectIdsByServant.containsKey(servant)) {
      throw PoaExceptions.badParam("UNIQUE_ID policy rejects duplicate servant activation");
    }
    LocalObjectReference<T> reference =
        bindReference(requestedObjectId, javaType, descriptor, null);
    activeObjectMap.put(reference.objectId(), new ActiveServant<>(servant, dispatcher));
    objectIdsByServant
        .computeIfAbsent(servant, ignored -> new ArrayList<>())
        .add(reference.objectId());
    return reference;
  }

  private <T> LocalObjectReference<T> bindReference(
      String requestedObjectId,
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      PoaServantDispatcher<Object> dispatcher) {
    PoaExceptions.requireNonNull(javaType, "javaType");
    PoaExceptions.requireNonNull(descriptor, "descriptor");
    LocalObjectReference<T> reference;
    if (requestedObjectId == null) {
      String[] assignedObjectId = new String[1];
      reference = orb.bind(javaType, descriptor, request -> dispatch(assignedObjectId[0], request));
      assignedObjectId[0] = reference.objectId();
    } else {
      reference =
          orb.bindWithObjectId(
              javaType,
              descriptor,
              requestedObjectId,
              request -> dispatch(requestedObjectId, request));
    }
    referencesByObjectId.put(reference.objectId(), reference);
    if (dispatcher != null) {
      dispatchersByObjectId.put(reference.objectId(), dispatcher);
    }
    return reference;
  }

  private Object dispatch(String objectId, LocalInvocationRequest request) throws Exception {
    manager.awaitDispatchPermission();
    if (policySet.threadPolicy() == PoaPolicySet.ThreadPolicy.SINGLE_THREAD_MODEL) {
      synchronized (this) {
        return dispatchWithPolicy(objectId, request);
      }
    }
    return dispatchWithPolicy(objectId, request);
  }

  private Object dispatchWithPolicy(String objectId, LocalInvocationRequest request)
      throws Exception {
    requireNotDestroyed();
    return switch (policySet.requestProcessingPolicy()) {
      case USE_ACTIVE_OBJECT_MAP_ONLY -> dispatchActiveOnly(objectId, request);
      case USE_DEFAULT_SERVANT -> dispatchDefaultServant(objectId, request);
      case USE_SERVANT_MANAGER -> dispatchServantManager(objectId, request);
    };
  }

  private Object dispatchActiveOnly(String objectId, LocalInvocationRequest request)
      throws Exception {
    ActiveServant<?> activeServant = activeObjectMap.get(objectId);
    if (activeServant == null) {
      throw PoaExceptions.objectNotExist("Unknown POA object id: " + objectId);
    }
    return activeServant.invoke(request);
  }

  private Object dispatchDefaultServant(String objectId, LocalInvocationRequest request)
      throws Exception {
    ActiveServant<?> activeServant = activeObjectMap.get(objectId);
    if (activeServant != null) {
      return activeServant.invoke(request);
    }
    DefaultServant<?> currentDefault = defaultServant;
    if (currentDefault == null) {
      throw PoaExceptions.objectNotExist("No default servant is registered");
    }
    return currentDefault.invoke(request);
  }

  private Object dispatchServantManager(String objectId, LocalInvocationRequest request)
      throws Exception {
    if (policySet.servantRetentionPolicy() == PoaPolicySet.ServantRetentionPolicy.RETAIN) {
      ActiveServant<?> activeServant = activeObjectMap.get(objectId);
      if (activeServant != null) {
        return activeServant.invoke(request);
      }
      if (servantActivator == null) {
        throw PoaExceptions.objectNotExist("No ServantActivator is registered");
      }
      PoaServantDispatcher<Object> dispatcher = servantManagerDispatcher(objectId);
      Object servant =
          PoaExceptions.requireNonNull(servantActivator.incarnate(this, objectId), "servant");
      ActiveServant<Object> incarnated = new ActiveServant<>(servant, dispatcher);
      activeObjectMap.put(objectId, incarnated);
      objectIdsByServant.computeIfAbsent(servant, ignored -> new ArrayList<>()).add(objectId);
      return incarnated.invoke(request);
    }
    if (servantLocator == null) {
      throw PoaExceptions.objectNotExist("No ServantLocator is registered");
    }
    PoaServantDispatcher<Object> dispatcher = servantManagerDispatcher(objectId);
    PoaServantLocatorResult result = servantLocator.preinvoke(this, objectId, request);
    Object outcome = null;
    Throwable failure = null;
    try {
      outcome = dispatcher.invoke(result.servant(), request);
      return outcome;
    } catch (RuntimeException | Error exception) {
      failure = exception;
      throw exception;
    } catch (Exception exception) {
      failure = exception;
      throw exception;
    } finally {
      servantLocator.postinvoke(this, objectId, request, result, outcome, failure);
    }
  }

  private void requireSystemId() {
    if (policySet.idAssignmentPolicy() != PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID) {
      throw PoaExceptions.badParam("This POA requires user-assigned object ids");
    }
  }

  private void requireUserId() {
    if (policySet.idAssignmentPolicy() != PoaPolicySet.IdAssignmentPolicy.USER_ID) {
      throw PoaExceptions.badParam("This POA requires system-assigned object ids");
    }
  }

  private void requireManagerActiveForActivation() {
    if (manager.state() == PoaManager.State.INACTIVE) {
      throw PoaExceptions.badInvOrder("POA manager is inactive");
    }
  }

  private void requireNotDestroyed() {
    if (destroyed) {
      throw PoaExceptions.badInvOrder("POA is destroyed");
    }
  }

  private void removeServantObjectId(Object servant, String objectId) {
    List<String> objectIds = objectIdsByServant.get(servant);
    if (objectIds == null) {
      return;
    }
    objectIds.remove(objectId);
    if (objectIds.isEmpty()) {
      objectIdsByServant.remove(servant);
    }
  }

  private PoaServantDispatcher<Object> servantManagerDispatcher(String objectId) {
    PoaServantDispatcher<Object> dispatcher = dispatchersByObjectId.get(objectId);
    if (dispatcher == null) {
      throw PoaExceptions.badParam("Servant-manager reference has no generated dispatcher");
    }
    return dispatcher;
  }

  private record ActiveServant<S>(S servant, PoaServantDispatcher<? super S> dispatcher) {

    private Object invoke(LocalInvocationRequest request) throws Exception {
      return dispatcher.invoke(servant, request);
    }
  }

  private record DefaultServant<S>(S servant, PoaServantDispatcher<? super S> dispatcher) {

    private Object invoke(LocalInvocationRequest request) throws Exception {
      return dispatcher.invoke(servant, request);
    }
  }
}
