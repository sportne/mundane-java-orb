package io.github.mundanej.mjo.naming;

import java.util.Objects;

/**
 * One binding listed from a naming context.
 *
 * @param name immediate component name
 * @param target bound target
 */
public record NamingBinding(NameComponent name, NamingBindingTarget target) {

  /** Creates a validated binding value. */
  public NamingBinding {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(target, "target");
  }
}
