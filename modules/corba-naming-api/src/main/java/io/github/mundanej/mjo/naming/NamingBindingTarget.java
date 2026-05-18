package io.github.mundanej.mjo.naming;

import io.github.mundanej.mjo.orb.LocalObjectReference;
import java.util.Objects;
import java.util.Optional;

/**
 * Local target bound in a naming context.
 *
 * @param kind binding target kind
 * @param objectReference local object reference for object bindings
 * @param context naming context for context bindings
 */
public record NamingBindingTarget(
    Kind kind, Optional<LocalObjectReference<?>> objectReference, Optional<NamingContext> context) {

  /** Binding target kind. */
  public enum Kind {
    /** A normal object reference. */
    OBJECT,
    /** A child naming context. */
    CONTEXT
  }

  /** Creates a validated binding target. */
  public NamingBindingTarget {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(objectReference, "objectReference");
    Objects.requireNonNull(context, "context");
    if (kind == Kind.OBJECT && (objectReference.isEmpty() || context.isPresent())) {
      throw new NamingException(
          NamingDiagnosticCodes.INVALID_NAME, "object target requires only an object reference");
    }
    if (kind == Kind.CONTEXT && (context.isEmpty() || objectReference.isPresent())) {
      throw new NamingException(
          NamingDiagnosticCodes.INVALID_NAME, "context target requires only a naming context");
    }
  }

  /** Creates an object binding target. */
  public static NamingBindingTarget object(LocalObjectReference<?> objectReference) {
    return new NamingBindingTarget(
        Kind.OBJECT,
        Optional.of(Objects.requireNonNull(objectReference, "objectReference")),
        Optional.empty());
  }

  /** Creates a context binding target. */
  public static NamingBindingTarget context(NamingContext context) {
    return new NamingBindingTarget(
        Kind.CONTEXT, Optional.empty(), Optional.of(Objects.requireNonNull(context, "context")));
  }
}
