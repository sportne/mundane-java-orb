package io.github.mundanej.mjo.naming;

/** Local NamingContext operations for the G6 CosNaming slice. */
public interface NamingContext {

  /** Binds a name to an object or context target. */
  void bind(NamingName name, NamingBindingTarget target);

  /** Binds or replaces a name with an object or context target. */
  void rebind(NamingName name, NamingBindingTarget target);

  /** Resolves a name to its current target. */
  NamingBindingTarget resolve(NamingName name);

  /** Removes a binding. */
  void unbind(NamingName name);

  /** Creates and binds a child naming context. */
  NamingContext bindNewContext(NamingName name);

  /** Lists immediate bindings, returning an iterator for remaining entries. */
  NamingListResult list(int howMany);

  /** Destroys this context when it is empty. */
  void destroy();

  /** Returns whether this context has been destroyed. */
  boolean isDestroyed();
}
