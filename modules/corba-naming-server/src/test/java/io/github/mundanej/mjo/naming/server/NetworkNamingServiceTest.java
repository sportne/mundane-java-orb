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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
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
  void persistentNamingCorbanameResolvesAfterForkedJvmRestart() throws Exception {
    Path store = tempDir.resolve("process-names.mjns");
    OrbIdentity identity = OrbIdentity.durable("g13-process-naming-orb");
    Ior object = durableFixtureIor(identity, "service");
    ProcessState first =
        startPersistentNaming(
            identity.requireDurableOrbId(),
            store,
            0,
            true,
            tempDir.resolve("naming-first-ready.properties"));
    String corbaname = first.ready().getProperty("corbaname");
    int port = Integer.parseInt(first.ready().getProperty("port"));

    try (first) {
      assertEquals(
          object,
          NetworkNamingClient.resolve(CorbanameUrl.parse(corbaname), IiopOptions.defaults()).ior());
    }

    ProcessState restarted =
        startPersistentNaming(
            identity.requireDurableOrbId(),
            store,
            port,
            false,
            tempDir.resolve("naming-second-ready.properties"));
    try (restarted) {
      assertEquals(
          object,
          NetworkNamingClient.resolve(CorbanameUrl.parse(corbaname), IiopOptions.defaults()).ior());
    }

    Process wrongOrb =
        startRejectedPersistentNaming(
            "g13-process-other-naming-orb", store, port, tempDir.resolve("naming-wrong-orb.log"));
    boolean exited = wrongOrb.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
    if (!exited) {
      wrongOrb.destroyForcibly();
    }
    assertTrue(exited, "wrong-ORB Naming restart did not exit");
    assertTrue(wrongOrb.exitValue() != 0, "wrong-ORB Naming restart unexpectedly succeeded");
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
  void persistenceStoreHardensDirectoryTempAndFallbackWritePaths() throws Exception {
    OrbIdentity identity = OrbIdentity.durable("naming-store-hardening-orb");
    Path directoryStore = tempDir.resolve("directory-store.mjns");
    Files.createDirectory(directoryStore);

    assertCode(
        NamingDiagnosticCodes.INVALID_NAME,
        () ->
            NetworkNamingService.bind(
                IiopEndpoint.loopback(0),
                IiopOptions.defaults(),
                NamingPersistenceOptions.of(identity, directoryStore)));

    Path cleanupStore = tempDir.resolve("cleanup-store.mjns");
    Path cleanupTemp = cleanupStore.resolveSibling(cleanupStore.getFileName() + ".tmp");
    Files.createDirectory(cleanupTemp);
    assertCode(
        NamingDiagnosticCodes.INVALID_NAME,
        () ->
            NetworkNamingService.bind(
                IiopEndpoint.loopback(0),
                IiopOptions.defaults(),
                NamingPersistenceOptions.of(identity, cleanupStore)));
    assertTrue(Files.notExists(cleanupTemp), "failed-write temp path should be cleaned up");

    Path durableStore = tempDir.resolve("durable-store.mjns");
    try (NetworkNamingService service =
        NetworkNamingService.bind(
            IiopEndpoint.loopback(0),
            IiopOptions.defaults(),
            NamingPersistenceOptions.of(identity, durableStore))) {
      assertEquals("127.0.0.1", service.endpoint().host());
      byte[] bytes = Files.readAllBytes(durableStore);
      assertEquals((byte) 'M', bytes[0]);
      assertEquals((byte) 'J', bytes[1]);
      assertEquals((byte) 'N', bytes[2]);
      assertEquals((byte) 'S', bytes[3]);
      assertTrue(
          Files.notExists(durableStore.resolveSibling(durableStore.getFileName() + ".tmp")),
          "successful write should not leave a temp file");
    }

    Path fallbackTemp = tempDir.resolve("fallback.tmp");
    Path fallbackStore = tempDir.resolve("fallback.mjns");
    Files.writeString(fallbackStore, "old");
    Files.writeString(fallbackTemp, "new");
    NamingPersistenceStore.replaceStore(fallbackTemp, fallbackStore, false);

    assertEquals("new", Files.readString(fallbackStore));
    assertTrue(Files.notExists(fallbackTemp), "fallback replacement should consume temp file");
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

  private ProcessState startPersistentNaming(
      String orbId, Path store, int port, boolean initialize, Path readyFile) throws Exception {
    Path stopFile = readyFile.resolveSibling(readyFile.getFileName() + ".stop");
    Path logFile = readyFile.resolveSibling(readyFile.getFileName() + ".log");
    List<String> command =
        namingCommand(
            PersistentNamingRestartServer.class.getName(),
            orbId,
            store,
            port,
            readyFile,
            stopFile,
            Boolean.toString(initialize));
    Process process =
        new ProcessBuilder(command)
            .redirectErrorStream(true)
            .redirectOutput(logFile.toFile())
            .start();
    return ProcessState.await(process, readyFile, stopFile, logFile);
  }

  private Process startRejectedPersistentNaming(String orbId, Path store, int port, Path logFile)
      throws Exception {
    List<String> command =
        namingCommand(
            PersistentNamingRestartServer.class.getName(),
            orbId,
            store,
            port,
            tempDir.resolve("wrong-orb-ready.properties"),
            tempDir.resolve("wrong-orb.stop"),
            Boolean.toString(false));
    return new ProcessBuilder(command)
        .redirectErrorStream(true)
        .redirectOutput(logFile.toFile())
        .start();
  }

  private static List<String> namingCommand(
      String className,
      String orbId,
      Path store,
      int port,
      Path readyFile,
      Path stopFile,
      String initialize) {
    return List.of(
        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
        "-cp",
        System.getProperty("java.class.path"),
        className,
        readyFile.toString(),
        stopFile.toString(),
        orbId,
        store.toString(),
        Integer.toString(port),
        initialize);
  }

  /** Child JVM entrypoint for process-level persistent Naming restart tests. */
  public static final class PersistentNamingRestartServer {

    private PersistentNamingRestartServer() {}

    /** Starts a persistent Naming Service until the stop file appears. */
    public static void main(String[] args) throws Exception {
      Path readyFile = Path.of(args[0]);
      Path stopFile = Path.of(args[1]);
      OrbIdentity identity = OrbIdentity.durable(args[2]);
      Path store = Path.of(args[3]);
      int port = Integer.parseInt(args[4]);
      boolean initialize = Boolean.parseBoolean(args[5]);
      NamingPersistenceOptions persistenceOptions = NamingPersistenceOptions.of(identity, store);
      try (NetworkNamingService service =
          NetworkNamingService.bind(
              IiopEndpoint.loopback(port), IiopOptions.defaults(), persistenceOptions)) {
        if (initialize) {
          Ior object = durableFixtureIor(identity, "service");
          try (NetworkNamingClient root =
              NetworkNamingClient.connect(service.ior(), IiopOptions.defaults())) {
            RemoteNamingBindingTarget child = root.bindNewContext(NamingName.parse("apps"));
            try (NetworkNamingClient childClient =
                NetworkNamingClient.connect(child.ior(), IiopOptions.defaults())) {
              childClient.bind(NamingName.parse("service"), object);
            }
          }
        }
        Properties properties = new Properties();
        properties.setProperty("host", service.endpoint().host());
        properties.setProperty("port", Integer.toString(service.endpoint().port()));
        properties.setProperty(
            "corbaname",
            "corbaname:" + service.corbaloc().substring("corbaloc:".length()) + "#apps/service");
        publishReady(readyFile, properties);
        while (!Files.exists(stopFile)) {
          Thread.sleep(50L);
        }
      }
    }
  }

  private static final class ProcessState implements AutoCloseable {

    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(15);

    private final Process process;
    private final Properties ready;
    private final Path stopFile;
    private final Path logFile;
    private boolean stopped;

    private ProcessState(Process process, Properties ready, Path stopFile, Path logFile) {
      this.process = process;
      this.ready = ready;
      this.stopFile = stopFile;
      this.logFile = logFile;
    }

    private static ProcessState await(Process process, Path readyFile, Path stopFile, Path logFile)
        throws Exception {
      long deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();
      while (System.nanoTime() < deadline) {
        if (Files.exists(readyFile)) {
          Properties ready = new Properties();
          try (InputStream input = Files.newInputStream(readyFile)) {
            ready.load(input);
          }
          return new ProcessState(process, ready, stopFile, logFile);
        }
        if (!process.isAlive()) {
          throw new AssertionError("child exited before ready: " + log(logFile));
        }
        Thread.sleep(50L);
      }
      process.destroyForcibly();
      throw new AssertionError("child did not become ready: " + log(logFile));
    }

    private Properties ready() {
      return ready;
    }

    private void stop() {
      if (stopped) {
        return;
      }
      try {
        Files.writeString(stopFile, "stop");
        if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
          process.destroyForcibly();
          throw new AssertionError("child did not stop: " + log(logFile));
        }
      } catch (InterruptedException exception) {
        process.destroyForcibly();
        Thread.currentThread().interrupt();
        throw new AssertionError(
            "interrupted while stopping child: " + safeLog(logFile), exception);
      } catch (Exception exception) {
        process.destroyForcibly();
        throw new AssertionError("failed to stop child: " + safeLog(logFile), exception);
      }
      if (process.exitValue() != 0) {
        throw new AssertionError(
            "child exited with " + process.exitValue() + ": " + safeLog(logFile));
      }
      stopped = true;
    }

    @Override
    public void close() {
      stop();
    }

    private static String log(Path logFile) throws Exception {
      return Files.exists(logFile) ? Files.readString(logFile) : "<no child log>";
    }

    private static String safeLog(Path logFile) {
      try {
        return log(logFile);
      } catch (Exception exception) {
        return "<unreadable child log: " + exception.getMessage() + ">";
      }
    }
  }

  private static void publishReady(Path readyFile, Properties properties) throws Exception {
    Path tempFile = readyFile.resolveSibling(readyFile.getFileName() + ".writing");
    try (OutputStream output = Files.newOutputStream(tempFile)) {
      properties.store(output, "ready");
    }
    try {
      Files.move(tempFile, readyFile, StandardCopyOption.ATOMIC_MOVE);
    } catch (java.io.IOException exception) {
      Files.move(tempFile, readyFile, StandardCopyOption.REPLACE_EXISTING);
    }
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
