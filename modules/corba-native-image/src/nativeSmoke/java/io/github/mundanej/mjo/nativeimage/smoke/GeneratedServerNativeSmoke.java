package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.modern.LocalInvocationDispatcher;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import java.util.List;

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
  }
}
