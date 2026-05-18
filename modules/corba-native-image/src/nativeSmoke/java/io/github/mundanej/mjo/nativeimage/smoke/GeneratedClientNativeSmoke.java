package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import java.util.List;

/** Native Image smoke entry point for generated-style client invocation. */
public final class GeneratedClientNativeSmoke {

  private GeneratedClientNativeSmoke() {}

  /** Runs a generated-style local client call through LocalOrb. */
  public static void main(String[] args) {
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<SmokeDescriptorFixtures.Greeter> reference =
        orb.bind(
            SmokeDescriptorFixtures.Greeter.class,
            SmokeDescriptorFixtures.GREETER,
            request -> "Hello " + request.arguments().getFirst());

    GreeterClient client = new GreeterClient(orb, reference);
    SmokeAssertions.requireEquals("Hello Ada", client.greet("Ada"), "generated client result");
    orb.shutdown();
    SmokeAssertions.require(orb.isShutdown(), "orb shutdown");
  }

  private record GreeterClient(
      LocalOrb orb, LocalObjectReference<SmokeDescriptorFixtures.Greeter> reference) {
    String greet(String name) {
      return (String) orb.invoke(reference, SmokeDescriptorFixtures.GREET, List.of(name));
    }
  }
}
