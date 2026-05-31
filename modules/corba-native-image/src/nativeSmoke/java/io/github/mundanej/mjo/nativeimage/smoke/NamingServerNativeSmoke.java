package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.iiop.IiopDiagnosticCodes;
import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopException;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.ior.CorbanameUrl;
import io.github.mundanej.mjo.ior.IiopProfile;
import io.github.mundanej.mjo.ior.IiopVersion;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.ObjectKey;
import io.github.mundanej.mjo.ior.TaggedProfile;
import io.github.mundanej.mjo.naming.NamingBindingTarget;
import io.github.mundanej.mjo.naming.NamingContext;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.naming.server.CorbanameResolver;
import io.github.mundanej.mjo.naming.server.LocalNamingService;
import io.github.mundanej.mjo.naming.server.NamingPersistenceOptions;
import io.github.mundanej.mjo.naming.server.NetworkNamingClient;
import io.github.mundanej.mjo.naming.server.NetworkNamingService;
import io.github.mundanej.mjo.orb.DurableObjectKey;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.orb.OrbIdentity;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Native Image smoke entry point for the local Naming Service. */
public final class NamingServerNativeSmoke {

  private NamingServerNativeSmoke() {}

  /** Installs NameService and resolves an object through corbaname:rir:. */
  public static void main(String[] args) throws Exception {
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

    runPersistentNamingRestartSmoke();
  }

  private static void runPersistentNamingRestartSmoke() throws Exception {
    IiopException lastBindFailure = null;
    for (int attempt = 0; attempt < 8; attempt++) {
      try {
        assertPersistentNamingRestartSmoke();
        return;
      } catch (IiopException exception) {
        if (!isBindFailure(exception)) {
          throw exception;
        }
        lastBindFailure = exception;
      }
    }
    throw new IllegalStateException("restart port was reused before rebind", lastBindFailure);
  }

  private static void assertPersistentNamingRestartSmoke() throws Exception {
    OrbIdentity identity = OrbIdentity.durable("native-naming-orb");
    Path store = Files.createTempDirectory("mjo-naming-native").resolve("names.mjns");
    NamingPersistenceOptions persistence = NamingPersistenceOptions.of(identity, store);
    Ior durableTarget = durableTarget(identity);
    IiopEndpoint endpoint;

    try (NetworkNamingService service =
            NetworkNamingService.bind(
                IiopEndpoint.loopback(0), IiopOptions.defaults(), persistence);
        NetworkNamingClient client =
            NetworkNamingClient.connect(service.ior(), IiopOptions.defaults())) {
      endpoint = service.endpoint();
      client.bindNewContext(NamingName.parse("apps"));
      client.bind(NamingName.parse("apps/service"), durableTarget);
      SmokeAssertions.requireEquals(
          durableTarget, client.resolve(NamingName.parse("apps/service")).ior(), "durable bind");
    }

    try (NetworkNamingService service = bindAfterRestart(endpoint, persistence)) {
      Ior resolved =
          NetworkNamingClient.resolve(
                  CorbanameUrl.parse(
                      "corbaname:"
                          + service.corbaloc().substring("corbaloc:".length())
                          + "#apps/service"),
                  IiopOptions.defaults())
              .ior();
      SmokeAssertions.requireEquals(durableTarget, resolved, "durable persisted resolve");
    }
  }

  private static NetworkNamingService bindAfterRestart(
      IiopEndpoint endpoint, NamingPersistenceOptions persistence) throws InterruptedException {
    IiopException lastBindFailure = null;
    for (int attempt = 0; attempt < 20; attempt++) {
      try {
        return NetworkNamingService.bind(endpoint, IiopOptions.defaults(), persistence);
      } catch (IiopException exception) {
        if (!isBindFailure(exception)) {
          throw exception;
        }
        lastBindFailure = exception;
        Thread.sleep(50L);
      }
    }
    throw new IllegalStateException("restart port was not released before rebind", lastBindFailure);
  }

  private static boolean isBindFailure(IiopException exception) {
    return IiopDiagnosticCodes.CONNECTION_FAILURE.equals(exception.code())
        && exception.getCause() instanceof java.net.BindException;
  }

  private static Ior durableTarget(OrbIdentity identity) {
    DurableObjectKey key =
        new DurableObjectKey(
            identity.requireDurableOrbId(),
            List.of("RootPOA", "native-naming"),
            "service".getBytes(StandardCharsets.US_ASCII),
            0);
    return new Ior(
        "IDL:example/NativeNaming:1.0",
        List.of(
            TaggedProfile.internetIop(
                new IiopProfile(
                    IiopVersion.V1_2, "127.0.0.1", 9, new ObjectKey(key.encode()), List.of()))));
  }
}
