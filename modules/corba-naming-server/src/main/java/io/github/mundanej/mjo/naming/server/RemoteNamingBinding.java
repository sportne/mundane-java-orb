package io.github.mundanej.mjo.naming.server;

import io.github.mundanej.mjo.naming.NameComponent;
import java.util.Objects;

/** One binding listed from a network Naming Service context. */
public record RemoteNamingBinding(NameComponent name, RemoteNamingBindingTarget target) {

  /** Creates a validated binding. */
  public RemoteNamingBinding {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(target, "target");
  }
}
