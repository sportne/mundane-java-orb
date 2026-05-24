package io.github.mundanej.mjo.naming.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.giop.GiopTargetAddress;
import io.github.mundanej.mjo.iiop.IiopClient;
import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.ior.CorbalocUrl;
import io.github.mundanej.mjo.ior.CorbanameUrl;
import io.github.mundanej.mjo.ior.IiopProfile;
import io.github.mundanej.mjo.ior.IiopVersion;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.ObjectKey;
import io.github.mundanej.mjo.ior.TaggedProfile;
import io.github.mundanej.mjo.naming.NameComponent;
import io.github.mundanej.mjo.naming.NamingDiagnosticCodes;
import io.github.mundanej.mjo.naming.NamingException;
import io.github.mundanej.mjo.naming.NamingName;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Loopback tests for the bounded network Naming Service lane. */
@Tag("unit")
final class NetworkNamingServiceTest {

  @Test
  void bindsRebindsResolvesListsAndUnbindsObjectIorsOverIiop() {
    Ior first = fixtureIor("first");
    Ior second = fixtureIor("second");

    try (NetworkNamingService service =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkNamingClient client =
            NetworkNamingClient.connect(
                CorbalocUrl.parse(service.corbaloc()), IiopOptions.defaults())) {
      client.bind(NamingName.parse("svc"), first);

      assertEquals(first, client.resolve(NamingName.parse("svc")).ior());
      assertEquals(
          List.of(
              new RemoteNamingBinding(
                  NameComponent.id("svc"), RemoteNamingBindingTarget.object(Ior.nullReference()))),
          client.list(10));

      client.rebind(NamingName.parse("svc"), second);
      assertEquals(second, client.resolve(NamingName.parse("svc")).ior());

      client.unbind(NamingName.parse("svc"));
      assertCode(NamingDiagnosticCodes.NOT_FOUND, () -> client.resolve(NamingName.parse("svc")));
    }
  }

  @Test
  void childContextsSupportStringifiedNameTraversalAndDestroy() {
    Ior object = fixtureIor("nested");

    try (NetworkNamingService service =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkNamingClient root =
            NetworkNamingClient.connect(service.ior(), IiopOptions.defaults())) {
      RemoteNamingBindingTarget child = root.bindNewContext(NamingName.parse("services"));
      try (NetworkNamingClient childClient =
          NetworkNamingClient.connect(child.ior(), IiopOptions.defaults())) {
        childClient.bind(NamingName.parse("echo"), object);
      }

      assertEquals(object, root.resolve(NamingName.parse("services/echo")).ior());
      assertEquals(
          object,
          NetworkNamingClient.resolve(
                  CorbanameUrl.parse(
                      "corbaname:"
                          + service.corbaloc().substring("corbaloc:".length())
                          + "#services/echo"),
                  IiopOptions.defaults())
              .ior());

      try (NetworkNamingClient childClient =
          NetworkNamingClient.connect(child.ior(), IiopOptions.defaults())) {
        childClient.unbind(NamingName.parse("echo"));
        childClient.destroy();
      }
      assertCode(
          NamingDiagnosticCodes.NOT_FOUND, () -> root.resolve(NamingName.parse("services/echo")));
    }
  }

  @Test
  void duplicateMissingAndUnsupportedLocationsAreDeterministic() {
    Ior object = fixtureIor("object");

    try (NetworkNamingService service =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkNamingClient client =
            NetworkNamingClient.connect(
                CorbalocUrl.parse(service.corbaloc()), IiopOptions.defaults())) {
      client.bind(NamingName.parse("svc"), object);

      assertCode(
          NamingDiagnosticCodes.ALREADY_BOUND, () -> client.bind(NamingName.parse("svc"), object));
      assertCode(NamingDiagnosticCodes.NOT_FOUND, () -> client.unbind(NamingName.parse("missing")));
    }

    assertCode(
        NamingDiagnosticCodes.UNSUPPORTED_LOCATION,
        () ->
            NetworkNamingClient.connect(
                CorbalocUrl.parse("corbaloc:rir:/NameService"), IiopOptions.defaults()));
  }

  @Test
  void profileAndReferenceTargetAddressesReachNamingContext() {
    Ior object = fixtureIor("target-address");

    try (NetworkNamingService service =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkNamingClient client =
            NetworkNamingClient.connect(service.ior(), IiopOptions.defaults());
        IiopClient rawClient = IiopClient.connect(service.endpoint(), IiopOptions.defaults())) {
      client.bind(NamingName.parse("svc"), object);

      assertEquals(
          object,
          NetworkNamingService.readTarget(
                  rawClient
                      .invoke(
                          request(
                              41,
                              GiopTargetAddress.profileAddr(service.ior().profiles().get(0)),
                              "svc"))
                      .body(),
                  RemoteNamingBindingTarget.Kind.OBJECT)
              .ior());
      assertEquals(
          object,
          NetworkNamingService.readTarget(
                  rawClient
                      .invoke(request(42, GiopTargetAddress.referenceAddr(0, service.ior()), "svc"))
                      .body(),
                  RemoteNamingBindingTarget.Kind.OBJECT)
              .ior());
    }
  }

  @Test
  void duplicateChildContextDoesNotCreateReachableOrphan() {
    try (NetworkNamingService service =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkNamingClient root =
            NetworkNamingClient.connect(service.ior(), IiopOptions.defaults())) {
      root.bindNewContext(NamingName.parse("ctx"));

      assertCode(
          NamingDiagnosticCodes.ALREADY_BOUND, () -> root.bindNewContext(NamingName.parse("ctx")));
      assertEquals(1, root.list(10).size());
    }
  }

  @Test
  void percentEncodedCorbalocKeysStayReversible() {
    try (NetworkNamingService service =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults());
        NetworkNamingClient client =
            NetworkNamingClient.connect(
                CorbalocUrl.parse(
                    "corbaloc:iiop:1.2@"
                        + service.endpoint().host()
                        + ":"
                        + service.endpoint().port()
                        + "/%C3"),
                IiopOptions.defaults())) {
      assertCode(NamingDiagnosticCodes.NOT_FOUND, () -> client.list(1));
    }
  }

  private static GiopRequest request(
      long requestId, GiopTargetAddress targetAddress, String stringifiedName) {
    return new GiopRequest(
        GiopHeader.forType(GiopMessageType.REQUEST),
        requestId,
        3,
        targetAddress,
        "resolve",
        List.of(),
        NetworkNamingService.writeName(NamingName.parse(stringifiedName)));
  }

  private static Ior fixtureIor(String objectKey) {
    return new Ior(
        "IDL:example/Fixture:1.0",
        List.of(
            TaggedProfile.internetIop(
                new IiopProfile(
                    IiopVersion.V1_2,
                    "127.0.0.1",
                    9,
                    new ObjectKey(objectKey.getBytes(StandardCharsets.US_ASCII)),
                    List.of()))));
  }

  private static void assertCode(Object expectedCode, ThrowingRunnable runnable) {
    NamingException exception = assertThrows(NamingException.class, runnable::run);
    assertEquals(expectedCode, exception.code());
  }

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run();
  }
}
