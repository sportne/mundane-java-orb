package io.github.mundanej.mjo.naming.server;

import io.github.mundanej.mjo.ior.CorbalocAddress;
import io.github.mundanej.mjo.ior.CorbanameUrl;
import io.github.mundanej.mjo.naming.NamingBindingTarget;
import io.github.mundanej.mjo.naming.NamingContext;
import io.github.mundanej.mjo.naming.NamingDiagnosticCodes;
import io.github.mundanej.mjo.naming.NamingException;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.orb.LocalOrb;
import java.util.Objects;

/** Resolves supported local {@code corbaname:} URLs through LocalOrb initial references. */
public final class CorbanameResolver {

  private final LocalOrb orb;

  /** Creates a resolver bound to one local ORB. */
  public CorbanameResolver(LocalOrb orb) {
    this.orb = Objects.requireNonNull(orb, "orb");
  }

  /** Resolves a supported local corbaname URL. */
  public NamingBindingTarget resolve(CorbanameUrl url) {
    Objects.requireNonNull(url, "url");
    if (url.location().addresses().size() != 1
        || url.location().addresses().get(0).kind() != CorbalocAddress.Kind.RIR) {
      throw new NamingException(
          NamingDiagnosticCodes.UNSUPPORTED_LOCATION,
          "only corbaname:rir: locations are supported locally");
    }
    NamingContext root =
        orb.resolveInitialReference(LocalNamingService.NAME_SERVICE, NamingContext.class);
    if (url.stringName().isEmpty()) {
      return NamingBindingTarget.context(root);
    }
    return root.resolve(NamingName.parse(url.stringName()));
  }
}
