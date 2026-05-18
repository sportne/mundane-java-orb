package io.github.mundanej.mjo.naming.server;

import io.github.mundanej.mjo.naming.NameComponent;
import io.github.mundanej.mjo.naming.NamingBinding;
import io.github.mundanej.mjo.naming.NamingBindingIterator;
import io.github.mundanej.mjo.naming.NamingBindingTarget;
import io.github.mundanej.mjo.naming.NamingContext;
import io.github.mundanej.mjo.naming.NamingDiagnosticCodes;
import io.github.mundanej.mjo.naming.NamingException;
import io.github.mundanej.mjo.naming.NamingListResult;
import io.github.mundanej.mjo.naming.NamingName;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** In-memory local NamingContext implementation. */
public final class LocalNamingContext implements NamingContext {

  private final Map<NameComponent, NamingBindingTarget> bindings = new LinkedHashMap<>();
  private boolean destroyed;

  private LocalNamingContext() {}

  /** Creates an empty local root naming context. */
  public static LocalNamingContext createRoot() {
    return new LocalNamingContext();
  }

  @Override
  public synchronized void bind(NamingName name, NamingBindingTarget target) {
    requireUsable();
    Objects.requireNonNull(target, "target");
    ResolvedParent parent = parentFor(name);
    parent.context().bindLeaf(parent.leaf(), target, false);
  }

  @Override
  public synchronized void rebind(NamingName name, NamingBindingTarget target) {
    requireUsable();
    Objects.requireNonNull(target, "target");
    ResolvedParent parent = parentFor(name);
    parent.context().bindLeaf(parent.leaf(), target, true);
  }

  @Override
  public synchronized NamingBindingTarget resolve(NamingName name) {
    requireUsable();
    Objects.requireNonNull(name, "name");
    LocalNamingContext context = this;
    List<NameComponent> components = name.components();
    for (int index = 0; index < components.size(); index++) {
      NameComponent component = components.get(index);
      NamingBindingTarget target = context.requireBinding(component);
      if (index == components.size() - 1) {
        return target;
      }
      context = requireContextTarget(component, target);
    }
    throw new NamingException(NamingDiagnosticCodes.INVALID_NAME, "name must not be empty");
  }

  @Override
  public synchronized void unbind(NamingName name) {
    requireUsable();
    ResolvedParent parent = parentFor(name);
    parent.context().unbindLeaf(parent.leaf());
  }

  @Override
  public synchronized NamingContext bindNewContext(NamingName name) {
    requireUsable();
    LocalNamingContext child = new LocalNamingContext();
    bind(name, NamingBindingTarget.context(child));
    return child;
  }

  @Override
  public synchronized NamingListResult list(int howMany) {
    requireUsable();
    if (howMany < 0) {
      throw new NamingException(
          NamingDiagnosticCodes.INVALID_NAME, "list count must not be negative");
    }
    List<NamingBinding> snapshot =
        bindings.entrySet().stream()
            .map(entry -> new NamingBinding(entry.getKey(), entry.getValue()))
            .toList();
    int inlineCount = Math.min(howMany, snapshot.size());
    List<NamingBinding> inline = snapshot.subList(0, inlineCount);
    List<NamingBinding> remaining = snapshot.subList(inlineCount, snapshot.size());
    Optional<NamingBindingIterator> iterator =
        remaining.isEmpty()
            ? Optional.empty()
            : Optional.of(new LocalNamingBindingIterator(remaining));
    return new NamingListResult(inline, iterator);
  }

  @Override
  public synchronized void destroy() {
    requireUsable();
    if (!bindings.isEmpty()) {
      throw new NamingException(NamingDiagnosticCodes.NOT_EMPTY, "naming context is not empty");
    }
    destroyed = true;
  }

  @Override
  public synchronized boolean isDestroyed() {
    return destroyed;
  }

  private synchronized void bindLeaf(
      NameComponent leaf, NamingBindingTarget target, boolean replace) {
    requireUsable();
    if (!replace && bindings.containsKey(leaf)) {
      throw new NamingException(
          NamingDiagnosticCodes.ALREADY_BOUND, "name component is already bound: " + leaf);
    }
    bindings.put(leaf, target);
  }

  private synchronized void unbindLeaf(NameComponent leaf) {
    requireUsable();
    if (bindings.remove(leaf) == null) {
      throw new NamingException(
          NamingDiagnosticCodes.NOT_FOUND, "name component is not bound: " + leaf);
    }
  }

  private ResolvedParent parentFor(NamingName name) {
    Objects.requireNonNull(name, "name");
    LocalNamingContext context = this;
    for (NameComponent component : name.parentComponents()) {
      NamingBindingTarget target = context.requireBinding(component);
      context = requireContextTarget(component, target);
    }
    return new ResolvedParent(context, name.leaf());
  }

  private synchronized NamingBindingTarget requireBinding(NameComponent component) {
    requireUsable();
    NamingBindingTarget target = bindings.get(component);
    if (target == null) {
      throw new NamingException(
          NamingDiagnosticCodes.NOT_FOUND, "name component is not bound: " + component);
    }
    return target;
  }

  private static LocalNamingContext requireContextTarget(
      NameComponent component, NamingBindingTarget target) {
    if (target.kind() != NamingBindingTarget.Kind.CONTEXT) {
      throw new NamingException(
          NamingDiagnosticCodes.NOT_CONTEXT, "name component is not a context: " + component);
    }
    NamingContext context = target.context().orElseThrow();
    if (!(context instanceof LocalNamingContext localContext)) {
      throw new NamingException(
          NamingDiagnosticCodes.NOT_CONTEXT, "context was not created by local naming server");
    }
    localContext.requireUsable();
    return localContext;
  }

  private synchronized void requireUsable() {
    if (destroyed) {
      throw new NamingException(
          NamingDiagnosticCodes.DESTROYED, "naming context has been destroyed");
    }
  }

  private record ResolvedParent(LocalNamingContext context, NameComponent leaf) {}

  private static final class LocalNamingBindingIterator implements NamingBindingIterator {

    private final List<NamingBinding> bindings;
    private int index;
    private boolean destroyed;

    private LocalNamingBindingIterator(List<NamingBinding> bindings) {
      this.bindings = List.copyOf(bindings);
    }

    @Override
    public synchronized Optional<NamingBinding> nextOne() {
      requireOpen();
      if (index >= bindings.size()) {
        return Optional.empty();
      }
      return Optional.of(bindings.get(index++));
    }

    @Override
    public synchronized List<NamingBinding> next(int count) {
      requireOpen();
      if (count < 0) {
        throw new NamingException(
            NamingDiagnosticCodes.INVALID_NAME, "iterator count must not be negative");
      }
      List<NamingBinding> result = new ArrayList<>();
      while (count > 0 && index < bindings.size()) {
        result.add(bindings.get(index++));
        count--;
      }
      return List.copyOf(result);
    }

    @Override
    public synchronized void destroy() {
      destroyed = true;
    }

    private void requireOpen() {
      if (destroyed) {
        throw new NamingException(
            NamingDiagnosticCodes.ITERATOR_CLOSED, "binding iterator is closed");
      }
    }
  }
}
