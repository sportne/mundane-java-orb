package io.github.mundanej.mjo.naming.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.BoundedLimit;
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
import io.github.mundanej.mjo.ior.StringifiedIor;
import io.github.mundanej.mjo.ior.TaggedProfile;
import io.github.mundanej.mjo.naming.NameComponent;
import io.github.mundanej.mjo.naming.NamingDiagnosticCodes;
import io.github.mundanej.mjo.naming.NamingException;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.orb.DurableObjectKey;
import io.github.mundanej.mjo.orb.OrbIdentity;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Loopback tests for the bounded network Naming Service lane. */
@Tag("unit")
final class NetworkNamingServiceTest {

  @TempDir Path tempDir;

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

  @Test
  void persistentNamingSurvivesRestartWithDurableObjectIorsAndCorbanameResolution() {
    OrbIdentity identity = OrbIdentity.durable("naming-persistent-orb");
    Path store = tempDir.resolve("names.mjns");
    NamingPersistenceOptions options = NamingPersistenceOptions.of(identity, store);
    Ior object = durableFixtureIor(identity, "service");
    IiopEndpoint endpoint;

    try (NetworkNamingService service =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), options);
        NetworkNamingClient root =
            NetworkNamingClient.connect(service.ior(), IiopOptions.defaults())) {
      endpoint = service.endpoint();
      RemoteNamingBindingTarget child = root.bindNewContext(NamingName.parse("apps"));
      try (NetworkNamingClient childClient =
          NetworkNamingClient.connect(child.ior(), IiopOptions.defaults())) {
        childClient.bind(NamingName.parse("service"), object);
      }

      assertTrue(Files.exists(store));
      assertEquals(object, root.resolve(NamingName.parse("apps/service")).ior());
    }

    try (NetworkNamingService service =
        NetworkNamingService.bind(endpoint, IiopOptions.defaults(), options)) {
      assertEquals(endpoint, service.endpoint());
      assertEquals(
          object,
          NetworkNamingClient.resolve(
                  CorbanameUrl.parse(
                      "corbaname:"
                          + service.corbaloc().substring("corbaloc:".length())
                          + "#apps/service"),
                  IiopOptions.defaults())
              .ior());
    }
  }

  @Test
  void persistentNamingRejectsTransientAndMalformedDurableIors() {
    NamingPersistenceOptions options =
        NamingPersistenceOptions.of(
            OrbIdentity.durable("naming-reject-orb"), tempDir.resolve("reject.mjns"));

    try (NetworkNamingService service =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), options);
        NetworkNamingClient client =
            NetworkNamingClient.connect(service.ior(), IiopOptions.defaults())) {
      assertCode(
          NamingDiagnosticCodes.INVALID_NAME,
          () -> client.bind(NamingName.parse("transient"), fixtureIor("transient")));
      assertCode(
          NamingDiagnosticCodes.INVALID_NAME,
          () -> client.bind(NamingName.parse("malformed"), malformedDurableIor()));
      assertCode(
          NamingDiagnosticCodes.INVALID_NAME,
          () ->
              client.bind(
                  NamingName.parse("wrong-orb"),
                  durableFixtureIor(OrbIdentity.durable("other-naming-orb"), "wrong")));
    }
  }

  @Test
  void failedPersistentSaveRollsBackVisibleState() {
    OrbIdentity identity = OrbIdentity.durable("naming-rollback-orb");
    NamingPersistenceOptions limited =
        new NamingPersistenceOptions(
            identity,
            tempDir.resolve("rollback.mjns"),
            new BoundedLimit("rollback-store", 512),
            new BoundedLimit("rollback-string", 16_384),
            new BoundedLimit("rollback-contexts", 16),
            new BoundedLimit("rollback-bindings", 16));

    try (NetworkNamingService service =
            NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), limited);
        NetworkNamingClient client =
            NetworkNamingClient.connect(service.ior(), IiopOptions.defaults())) {
      assertCode(
          NamingDiagnosticCodes.INVALID_NAME,
          () ->
              client.bind(
                  NamingName.parse("oversized"), durableFixtureIor(identity, "x".repeat(2048))));
      assertCode(
          NamingDiagnosticCodes.NOT_FOUND, () -> client.resolve(NamingName.parse("oversized")));
    }
  }

  @Test
  void persistenceStoreRejectsCorruptionOversizeAndTraversal() throws Exception {
    OrbIdentity identity = OrbIdentity.durable("naming-store-orb");
    Path corrupt = tempDir.resolve("corrupt.mjns");
    Files.write(corrupt, new byte[] {'M', 'J', 'N', 'S', 99});

    assertCode(
        NamingDiagnosticCodes.INVALID_NAME,
        () ->
            NetworkNamingService.bind(
                IiopEndpoint.loopback(0),
                IiopOptions.defaults(),
                NamingPersistenceOptions.of(identity, corrupt)));

    Path oversized = tempDir.resolve("oversized.mjns");
    Files.write(oversized, new byte[] {1, 2, 3, 4});
    NamingPersistenceOptions limited =
        new NamingPersistenceOptions(
            identity,
            oversized,
            new BoundedLimit("tiny-store", 3),
            new BoundedLimit("string", 64),
            new BoundedLimit("contexts", 4),
            new BoundedLimit("bindings", 4));
    assertCode(
        NamingDiagnosticCodes.INVALID_NAME,
        () -> NetworkNamingService.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), limited));

    assertCode(
        NamingDiagnosticCodes.INVALID_NAME,
        () -> NamingPersistenceOptions.of(identity, tempDir.resolve("..").resolve("evil.mjns")));
  }

  @Test
  void persistenceStoreRejectsWrongContextNamespaceAndMalformedUtf8() throws Exception {
    OrbIdentity identity = OrbIdentity.durable("naming-hostile-store-orb");
    Path wrongContext = tempDir.resolve("wrong-context.mjns");
    Files.write(
        wrongContext,
        rawStore(
            identity,
            durableIor(
                identity,
                "IDL:omg.org/CosNaming/NamingContextExt:1.0",
                List.of("RootPOA", "Other"),
                "NameService"),
            false));
    assertCode(
        NamingDiagnosticCodes.INVALID_NAME,
        () ->
            NetworkNamingService.bind(
                IiopEndpoint.loopback(0),
                IiopOptions.defaults(),
                NamingPersistenceOptions.of(identity, wrongContext)));

    Path malformedUtf8 = tempDir.resolve("malformed-utf8.mjns");
    Files.write(malformedUtf8, rawStoreWithMalformedName(identity));
    assertCode(
        NamingDiagnosticCodes.INVALID_NAME,
        () ->
            NetworkNamingService.bind(
                IiopEndpoint.loopback(0),
                IiopOptions.defaults(),
                NamingPersistenceOptions.of(identity, malformedUtf8)));
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

  private static Ior durableFixtureIor(OrbIdentity identity, String objectKey) {
    return durableIor(
        identity, "IDL:example/Fixture:1.0", List.of("RootPOA", "fixtures"), objectKey);
  }

  private static Ior durableIor(
      OrbIdentity identity, String typeId, List<String> poaPath, String objectKey) {
    DurableObjectKey durableObjectKey =
        new DurableObjectKey(
            identity.requireDurableOrbId(),
            poaPath,
            objectKey.getBytes(StandardCharsets.US_ASCII),
            0);
    return new Ior(
        typeId,
        List.of(
            TaggedProfile.internetIop(
                new IiopProfile(
                    IiopVersion.V1_2,
                    "127.0.0.1",
                    9,
                    new ObjectKey(durableObjectKey.encode()),
                    List.of()))));
  }

  private static Ior durableNamingContextIor(OrbIdentity identity, String objectKey) {
    return durableIor(
        identity,
        "IDL:omg.org/CosNaming/NamingContextExt:1.0",
        List.of("RootPOA", "NameService"),
        objectKey);
  }

  private static Ior malformedDurableIor() {
    return new Ior(
        "IDL:example/Fixture:1.0",
        List.of(
            TaggedProfile.internetIop(
                new IiopProfile(
                    IiopVersion.V1_2,
                    "127.0.0.1",
                    9,
                    new ObjectKey(new byte[] {'M', 'J', 'O', 'K', 1}),
                    List.of()))));
  }

  private static void assertCode(Object expectedCode, ThrowingRunnable runnable) {
    NamingException exception = assertThrows(NamingException.class, runnable::run);
    assertEquals(expectedCode, exception.code());
  }

  private static byte[] rawStore(OrbIdentity identity, Ior contextIor, boolean withEmptyBinding) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.writeBytes(new byte[] {'M', 'J', 'N', 'S'});
    output.write(1);
    writeString(output, identity.requireDurableOrbId());
    writeInt(output, 1);
    writeUnsignedShort(output, 1);
    writeString(output, StringifiedIor.format(contextIor));
    output.write(0);
    writeUnsignedShort(output, withEmptyBinding ? 1 : 0);
    if (withEmptyBinding) {
      writeString(output, "svc");
      writeString(output, "");
      output.write(0);
      writeString(output, StringifiedIor.format(durableFixtureIor(identity, "target")));
    }
    return output.toByteArray();
  }

  private static byte[] rawStoreWithMalformedName(OrbIdentity identity) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.writeBytes(new byte[] {'M', 'J', 'N', 'S'});
    output.write(1);
    writeString(output, identity.requireDurableOrbId());
    writeInt(output, 1);
    writeUnsignedShort(output, 1);
    writeString(output, StringifiedIor.format(durableNamingContextIor(identity, "NameService")));
    output.write(0);
    writeUnsignedShort(output, 1);
    writeInt(output, 1);
    output.write(0xC3);
    writeString(output, "");
    output.write(0);
    writeString(output, StringifiedIor.format(durableFixtureIor(identity, "target")));
    return output.toByteArray();
  }

  private static void writeString(ByteArrayOutputStream output, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    writeInt(output, bytes.length);
    output.writeBytes(bytes);
  }

  private static void writeInt(ByteArrayOutputStream output, int value) {
    output.write((value >>> 24) & 0xFF);
    output.write((value >>> 16) & 0xFF);
    output.write((value >>> 8) & 0xFF);
    output.write(value & 0xFF);
  }

  private static void writeUnsignedShort(ByteArrayOutputStream output, int value) {
    output.write((value >>> 8) & 0xFF);
    output.write(value & 0xFF);
  }

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run();
  }
}
