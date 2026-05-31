package io.github.mundanej.mjo.orb;

import io.github.mundanej.mjo.modern.LocalInvocationDispatcher;
import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.omg.CORBA.SystemException;

/** Minimal in-process ORB for generated-style local object invocation. */
public final class LocalOrb {

  private static final AtomicLong NEXT_OWNER_TOKEN = new AtomicLong(1L);

  private final long ownerToken = NEXT_OWNER_TOKEN.getAndIncrement();
  private final OrbIdentity identity;
  private final DurablePoaPathRegistry durablePoaPaths;
  private final Map<String, Binding> bindings = new LinkedHashMap<>();
  private final Map<String, InitialReference> initialReferences = new LinkedHashMap<>();
  private long nextObjectNumber = 1L;
  private boolean shutdown;

  private LocalOrb(OrbIdentity identity) {
    this.identity = LocalExceptionMapper.requireNonNull(identity, "identity");
    this.durablePoaPaths = new DurablePoaPathRegistry(this.identity);
  }

  /** Creates an active local ORB instance. */
  public static LocalOrb create() {
    return new LocalOrb(OrbIdentity.transientLocal());
  }

  /** Creates an active local ORB instance with an explicit identity. */
  public static LocalOrb create(OrbIdentity identity) {
    return new LocalOrb(identity);
  }

  /** Returns this ORB's configured identity. */
  public OrbIdentity identity() {
    return identity;
  }

  /** Returns the durable POA path registry owned by this ORB. */
  public DurablePoaPathRegistry durablePoaPaths() {
    return durablePoaPaths;
  }

  /** Binds a generated-style dispatcher and returns a local object reference. */
  public synchronized <T> LocalObjectReference<T> bind(
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      LocalInvocationDispatcher dispatcher) {
    requireActive();
    LocalExceptionMapper.requireNonNull(javaType, "javaType");
    LocalExceptionMapper.requireNonNull(descriptor, "descriptor");
    LocalExceptionMapper.requireNonNull(dispatcher, "dispatcher");
    String objectId = "local-" + nextObjectNumber++;
    return bindWithCheckedId(javaType, descriptor, dispatcher, objectId, objectId, null);
  }

  /** Binds a generated-style dispatcher to a caller-supplied local object id. */
  public synchronized <T> LocalObjectReference<T> bindWithObjectId(
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      String objectId,
      LocalInvocationDispatcher dispatcher) {
    requireActive();
    LocalExceptionMapper.requireNonNull(javaType, "javaType");
    LocalExceptionMapper.requireNonNull(descriptor, "descriptor");
    LocalExceptionMapper.requireNonNull(dispatcher, "dispatcher");
    String checkedObjectId = requireNonBlank(objectId, "objectId");
    if (bindings.containsKey(checkedObjectId)) {
      throw LocalExceptionMapper.badParam("Local object id is already bound: " + checkedObjectId);
    }
    return bindWithCheckedId(
        javaType, descriptor, dispatcher, checkedObjectId, checkedObjectId, null);
  }

  /** Binds a generated-style dispatcher to a persistent POA object id and durable key. */
  public synchronized <T> LocalObjectReference<T> bindWithDurableObjectKey(
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      String objectId,
      DurableObjectKey durableObjectKey,
      LocalInvocationDispatcher dispatcher) {
    requireActive();
    LocalExceptionMapper.requireNonNull(javaType, "javaType");
    LocalExceptionMapper.requireNonNull(descriptor, "descriptor");
    LocalExceptionMapper.requireNonNull(durableObjectKey, "durableObjectKey");
    LocalExceptionMapper.requireNonNull(dispatcher, "dispatcher");
    String checkedObjectId = requireNonBlank(objectId, "objectId");
    if (!identity.durable()) {
      throw LocalExceptionMapper.badParam("Durable object keys require a durable ORB identity");
    }
    if (!identity.requireDurableOrbId().equals(durableObjectKey.orbId())) {
      throw LocalExceptionMapper.badParam("Durable object key belongs to a different ORB");
    }
    String bindingId = durableBindingId(durableObjectKey);
    if (bindings.containsKey(bindingId)) {
      throw LocalExceptionMapper.badParam("Local object id is already bound: " + checkedObjectId);
    }
    return bindWithCheckedId(
        javaType, descriptor, dispatcher, bindingId, checkedObjectId, durableObjectKey);
  }

  /** Removes one local object binding. Missing object ids are ignored. */
  public synchronized void unbind(String objectId) {
    requireActive();
    bindings.remove(requireNonBlank(objectId, "objectId"));
  }

  /** Removes one local object binding by reference identity. Missing references are ignored. */
  public synchronized void unbindReference(LocalObjectReference<?> reference) {
    requireActive();
    LocalExceptionMapper.requireNonNull(reference, "reference");
    if (reference.ownerToken() != ownerToken) {
      throw LocalExceptionMapper.objectNotExist(
          "Local object reference belongs to a different local ORB");
    }
    bindings.remove(reference.bindingId());
  }

  /** Registers a typed local initial reference. */
  public synchronized <T> void registerInitialReference(
      String name, Class<T> javaType, T reference) {
    requireActive();
    String checkedName = requireNonBlank(name, "name");
    LocalExceptionMapper.requireNonNull(javaType, "javaType");
    LocalExceptionMapper.requireNonNull(reference, "reference");
    if (!javaType.isInstance(reference)) {
      throw LocalExceptionMapper.badParam(
          "Initial reference does not implement " + javaType.getName() + ": " + checkedName);
    }
    if (initialReferences.containsKey(checkedName)) {
      throw LocalExceptionMapper.badParam("Initial reference already registered: " + checkedName);
    }
    initialReferences.put(checkedName, new InitialReference(javaType, reference));
  }

  /** Resolves a typed local initial reference. */
  public synchronized <T> T resolveInitialReference(String name, Class<T> javaType) {
    requireActive();
    String checkedName = requireNonBlank(name, "name");
    LocalExceptionMapper.requireNonNull(javaType, "javaType");
    InitialReference reference = initialReferences.get(checkedName);
    if (reference == null) {
      throw LocalExceptionMapper.badParam("Initial reference is not registered: " + checkedName);
    }
    if (!javaType.isInstance(reference.value())) {
      throw LocalExceptionMapper.badParam(
          "Initial reference " + checkedName + " is not assignable to " + javaType.getName());
    }
    return javaType.cast(reference.value());
  }

  /** Removes a typed local initial reference. */
  public synchronized void removeInitialReference(String name) {
    requireActive();
    String checkedName = requireNonBlank(name, "name");
    if (initialReferences.remove(checkedName) == null) {
      throw LocalExceptionMapper.badParam("Initial reference is not registered: " + checkedName);
    }
  }

  private <T> LocalObjectReference<T> bindWithCheckedId(
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      LocalInvocationDispatcher dispatcher,
      String bindingId,
      String objectId,
      DurableObjectKey durableObjectKey) {
    LocalObjectReference<T> reference =
        new LocalObjectReference<>(
            ownerToken, bindingId, objectId, javaType, descriptor, durableObjectKey);
    bindings.put(bindingId, new Binding(descriptor, dispatcher));
    return reference;
  }

  /** Invokes one operation through the local object reference. */
  public synchronized Object invoke(
      LocalObjectReference<?> reference, IdlOperationDescriptor operation, List<Object> arguments) {
    requireActive();
    LocalExceptionMapper.requireNonNull(reference, "reference");
    LocalExceptionMapper.requireNonNull(operation, "operation");
    LocalExceptionMapper.requireNonNull(arguments, "arguments");
    Binding binding = bindingFor(reference);
    validateOperation(binding.descriptor(), operation, arguments);
    try {
      return binding
          .dispatcher()
          .invoke(new LocalInvocationRequest(binding.descriptor(), operation, arguments));
    } catch (SystemException exception) {
      throw exception;
    } catch (Exception exception) {
      throw LocalExceptionMapper.mapDispatcherException(operation, exception);
    }
  }

  /** Shuts this local ORB down. Repeated shutdown calls are safe. */
  public synchronized void shutdown() {
    shutdown = true;
    durablePoaPaths.close();
    bindings.clear();
    initialReferences.clear();
  }

  /** Returns whether this local ORB has been shut down. */
  public synchronized boolean isShutdown() {
    return shutdown;
  }

  private Binding bindingFor(LocalObjectReference<?> reference) {
    if (reference.ownerToken() != ownerToken) {
      throw LocalExceptionMapper.objectNotExist(
          "Local object reference belongs to a different local ORB");
    }
    Binding binding = bindings.get(reference.bindingId());
    if (binding == null) {
      throw LocalExceptionMapper.objectNotExist(
          "Unknown local object reference: " + reference.objectId());
    }
    return binding;
  }

  private static void validateOperation(
      IdlGeneratedTypeDescriptor descriptor,
      IdlOperationDescriptor operation,
      List<Object> arguments) {
    if (!descriptor.operations().contains(operation)) {
      throw LocalExceptionMapper.badOperation(
          "Operation is not declared by target descriptor: " + operation.name());
    }
    int inputArgumentCount =
        (int)
            operation.parameters().stream()
                .filter(parameter -> parameter.mode() != IdlParameterMode.OUT)
                .count();
    if (inputArgumentCount != arguments.size()) {
      throw LocalExceptionMapper.badParam(
          "Operation "
              + operation.name()
              + " expects "
              + inputArgumentCount
              + " argument(s), got "
              + arguments.size());
    }
  }

  private void requireActive() {
    if (shutdown) {
      throw LocalExceptionMapper.badInvOrder("Local ORB is shut down");
    }
  }

  private static String requireNonBlank(String value, String name) {
    LocalExceptionMapper.requireNonNull(value, name);
    if (value.isBlank()) {
      throw LocalExceptionMapper.badParam(name + " must not be blank");
    }
    return value;
  }

  private static String durableBindingId(DurableObjectKey durableObjectKey) {
    return "durable:"
        + Base64.getUrlEncoder().withoutPadding().encodeToString(durableObjectKey.encode());
  }

  private record Binding(
      IdlGeneratedTypeDescriptor descriptor, LocalInvocationDispatcher dispatcher) {}

  private record InitialReference(Class<?> javaType, Object value) {}
}
