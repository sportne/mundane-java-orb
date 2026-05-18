package io.github.mundanej.mjo.naming.server;

import io.github.mundanej.mjo.naming.NamingContext;
import io.github.mundanej.mjo.orb.LocalOrb;
import java.util.Objects;

/** Installs the local Naming Service into a LocalOrb initial reference table. */
public final class LocalNamingService {

  /** Standard local initial-reference name for the root naming context. */
  public static final String NAME_SERVICE = "NameService";

  private LocalNamingService() {}

  /** Creates and registers a local root NamingContext as {@code NameService}. */
  public static NamingContext install(LocalOrb orb) {
    Objects.requireNonNull(orb, "orb");
    NamingContext root = LocalNamingContext.createRoot();
    orb.registerInitialReference(NAME_SERVICE, NamingContext.class, root);
    return root;
  }
}
