package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.modern.LocalInvocationDispatcher;
import io.github.mundanej.mjo.orb.DurableObjectKey;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.orb.OrbIdentity;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;

/** Native Image smoke entry point for generated-style server dispatch. */
public final class GeneratedServerNativeSmoke {

  private GeneratedServerNativeSmoke() {}

  /** Runs a generated-style dispatcher through LocalOrb. */
  public static void main(String[] args) {
    LocalOrb orb = LocalOrb.create();
    LocalInvocationDispatcher dispatcher =
        request -> {
          SmokeAssertions.requireEquals(
              SmokeDescriptorFixtures.GREET, request.operation(), "operation descriptor");
          SmokeAssertions.requireEquals(List.of("Grace"), request.arguments(), "arguments");
          return "Hello Grace";
        };

    LocalObjectReference<SmokeDescriptorFixtures.Greeter> reference =
        orb.bindWithObjectId(
            SmokeDescriptorFixtures.Greeter.class,
            SmokeDescriptorFixtures.GREETER,
            "native-server",
            dispatcher);

    SmokeAssertions.requireEquals(
        "Hello Grace",
        orb.invoke(reference, SmokeDescriptorFixtures.GREET, List.of("Grace")),
        "server dispatch result");
    SmokeAssertions.requireEquals("native-server", reference.objectId(), "object id");

    LocalOrb durableOrb = LocalOrb.create(OrbIdentity.durable("native-registry-orb"));
    durableOrb.durablePoaPaths().register(List.of("RootPOA", "native"));
    durableOrb
        .durablePoaPaths()
        .requireRegistered(
            DurableObjectKey.fromPoaPath(
                "native-registry-orb", "/RootPOA/native", ascii("native-object"), 0));
    SmokeAssertions.requireThrows(
        BAD_PARAM.class,
        () -> durableOrb.durablePoaPaths().register(List.of("RootPOA", "native")),
        "duplicate durable POA path registration");
    SmokeAssertions.requireThrows(
        BAD_PARAM.class,
        () -> LocalOrb.create().durablePoaPaths().register(List.of("RootPOA")),
        "transient ORB durable POA path registration");
    durableOrb.shutdown();
    SmokeAssertions.requireThrows(
        BAD_INV_ORDER.class,
        () -> durableOrb.durablePoaPaths().contains(List.of("RootPOA", "native")),
        "durable POA path registry shutdown");
  }

  private static byte[] ascii(String value) {
    return value.getBytes(StandardCharsets.US_ASCII);
  }
}
