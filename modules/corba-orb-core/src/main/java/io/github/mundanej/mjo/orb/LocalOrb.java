package io.github.mundanej.mjo.orb;

import io.github.mundanej.mjo.modern.LocalInvocationDispatcher;
import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Minimal in-process ORB for generated-style local object invocation. */
public final class LocalOrb {

  private static final AtomicLong NEXT_OWNER_TOKEN = new AtomicLong(1L);

  private final long ownerToken = NEXT_OWNER_TOKEN.getAndIncrement();
  private final Map<String, Binding> bindings = new LinkedHashMap<>();
  private long nextObjectNumber = 1L;
  private boolean shutdown;

  private LocalOrb() {}

  /** Creates an active local ORB instance. */
  public static LocalOrb create() {
    return new LocalOrb();
  }

  /** Binds a generated-style dispatcher and returns a local object reference. */
  public synchronized <T> LocalObjectReference<T> bind(
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      LocalInvocationDispatcher dispatcher) {
    requireActive();
    Objects.requireNonNull(javaType, "javaType");
    Objects.requireNonNull(descriptor, "descriptor");
    Objects.requireNonNull(dispatcher, "dispatcher");
    String objectId = "local-" + nextObjectNumber++;
    LocalObjectReference<T> reference =
        new LocalObjectReference<>(ownerToken, objectId, javaType, descriptor);
    bindings.put(objectId, new Binding(descriptor, dispatcher));
    return reference;
  }

  /** Invokes one operation through the local object reference. */
  public synchronized Object invoke(
      LocalObjectReference<?> reference, IdlOperationDescriptor operation, List<Object> arguments) {
    requireActive();
    Objects.requireNonNull(reference, "reference");
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(arguments, "arguments");
    Binding binding = bindingFor(reference);
    validateOperation(binding.descriptor(), operation, arguments);
    return binding
        .dispatcher()
        .invoke(new LocalInvocationRequest(binding.descriptor(), operation, arguments));
  }

  /** Shuts this local ORB down. Repeated shutdown calls are safe. */
  public synchronized void shutdown() {
    shutdown = true;
    bindings.clear();
  }

  /** Returns whether this local ORB has been shut down. */
  public synchronized boolean isShutdown() {
    return shutdown;
  }

  private Binding bindingFor(LocalObjectReference<?> reference) {
    if (reference.ownerToken() != ownerToken) {
      throw new LocalOrbException("Local object reference belongs to a different local ORB");
    }
    Binding binding = bindings.get(reference.objectId());
    if (binding == null) {
      throw new LocalOrbException("Unknown local object reference: " + reference.objectId());
    }
    return binding;
  }

  private static void validateOperation(
      IdlGeneratedTypeDescriptor descriptor,
      IdlOperationDescriptor operation,
      List<Object> arguments) {
    if (!descriptor.operations().contains(operation)) {
      throw new LocalOrbException(
          "Operation is not declared by target descriptor: " + operation.name());
    }
    if (operation.parameters().size() != arguments.size()) {
      throw new LocalOrbException(
          "Operation "
              + operation.name()
              + " expects "
              + operation.parameters().size()
              + " argument(s), got "
              + arguments.size());
    }
  }

  private void requireActive() {
    if (shutdown) {
      throw new LocalOrbException("Local ORB is shut down");
    }
  }

  private record Binding(
      IdlGeneratedTypeDescriptor descriptor, LocalInvocationDispatcher dispatcher) {}
}
