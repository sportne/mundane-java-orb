package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.ior.CorbanameUrl;
import io.github.mundanej.mjo.naming.NamingBindingTarget;
import io.github.mundanej.mjo.naming.NamingContext;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.naming.server.CorbanameResolver;
import io.github.mundanej.mjo.naming.server.LocalNamingService;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;

/** Native Image smoke entry point for the local Naming Service. */
public final class NamingServerNativeSmoke {

  private NamingServerNativeSmoke() {}

  /** Installs NameService and resolves an object through corbaname:rir:. */
  public static void main(String[] args) {
    LocalOrb orb = LocalOrb.create();
    NamingContext root = LocalNamingService.install(orb);
    root.bindNewContext(NamingName.parse("apps"));

    LocalObjectReference<SmokeDescriptorFixtures.Greeter> object =
        orb.bind(
            SmokeDescriptorFixtures.Greeter.class,
            SmokeDescriptorFixtures.GREETER,
            request -> "Hello " + request.arguments().getFirst());
    root.bind(NamingName.parse("apps/service"), NamingBindingTarget.object(object));

    NamingBindingTarget target =
        new CorbanameResolver(orb).resolve(CorbanameUrl.parse("corbaname:rir:#apps/service"));
    SmokeAssertions.requireEquals(NamingBindingTarget.Kind.OBJECT, target.kind(), "target kind");
    SmokeAssertions.requireEquals(
        object.objectId(), target.objectReference().orElseThrow().objectId(), "resolved object");
  }
}
