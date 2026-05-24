package io.github.mundanej.mjo.naming.server;

import io.github.mundanej.mjo.ior.Ior;
import java.util.Objects;

/** Network Naming Service target represented by an IOR. */
public record RemoteNamingBindingTarget(Kind kind, Ior ior) {

  /** Binding target kind. */
  public enum Kind {
    /** A normal object reference. */
    OBJECT,
    /** A child naming context reference. */
    CONTEXT
  }

  /** Creates a validated network target. */
  public RemoteNamingBindingTarget {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(ior, "ior");
  }

  /** Creates an object target. */
  public static RemoteNamingBindingTarget object(Ior ior) {
    return new RemoteNamingBindingTarget(Kind.OBJECT, ior);
  }

  /** Creates a naming context target. */
  public static RemoteNamingBindingTarget context(Ior ior) {
    return new RemoteNamingBindingTarget(Kind.CONTEXT, ior);
  }
}
