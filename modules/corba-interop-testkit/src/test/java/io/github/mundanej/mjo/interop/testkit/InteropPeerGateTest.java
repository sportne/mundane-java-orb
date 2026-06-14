package io.github.mundanej.mjo.interop.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class InteropPeerGateTest {
  private static final List<String> REAL_PEERS =
      List.of("ace-tao", "glassfish-orb", "jacorb", "jboss-openjdk-orb");
  private static final String FIXTURE_PEER = "fixture-peer";
  private static final String FIXTURE_CACHE_ENTRY = "maven/example/fixture/1.0/fixture-1.0.jar";
  private static final String FIXTURE_CONTENT = "approved artifact\n";
  private static final String DIGEST_PINNED_BASE_IMAGE =
      "example@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

  @TempDir private Path temporaryDirectory;

  @Test
  void actualPeerManifestsAndApprovalRecordsValidateWithoutLocalCache() throws Exception {
    assertSuccess(run(command("validate-manifests"), Map.of()));

    CommandResult gates = run(command("validate-gates"), Map.of());

    assertSuccess(gates);
    for (String peer : REAL_PEERS) {
      assertTrue(gates.output().contains(peer + ": interop/approvals/" + peer + ".approval.yaml"));
    }
  }

  @Test
  void actualPeerManifestsDeclareRmiIiopAndDurablePeerScenarios() throws Exception {
    for (String peer : REAL_PEERS) {
      String manifest =
          Files.readString(repoRoot().resolve("interop/peers/" + peer + "/peer.yaml"));

      assertTrue(manifest.contains("scenarioGroups:"), manifest);
      assertTrue(manifest.contains("  - rmi-iiop"), manifest);
      assertTrue(manifest.contains("  - g13-durable-ior-peer-client-restart"), manifest);
      assertTrue(manifest.contains("  - g13-durable-naming-peer-client-restart"), manifest);
      assertTrue(manifest.contains("support: durable-ior-peer-client-restart"), manifest);
      assertTrue(manifest.contains("support: durable-naming-peer-client-restart"), manifest);
      assertTrue(manifest.contains("  - time-service"), manifest);
      assertTrue(manifest.contains("time-service:"), manifest);
      assertTrue(manifest.contains("idl: interop/idl/time-service.idl"), manifest);
      assertTrue(manifest.contains("support: live-time-service-smoke"), manifest);
      assertTrue(manifest.contains("time-service-checked"), manifest);
      assertTrue(manifest.contains("  - event-service"), manifest);
      assertTrue(manifest.contains("event-service:"), manifest);
      assertTrue(manifest.contains("idl: interop/idl/event-service.idl"), manifest);
      assertTrue(manifest.contains("support: event-service-metadata-dry-run"), manifest);
      assertTrue(manifest.contains("  - notification-service"), manifest);
      assertTrue(manifest.contains("notification-service:"), manifest);
      assertTrue(manifest.contains("idl: interop/idl/notification-service.idl"), manifest);
      assertTrue(manifest.contains("support: notification-service-metadata-dry-run"), manifest);
    }
    assertTrue(
        Files.exists(repoRoot().resolve("interop/idl/rmi-iiop/Calculator.idl")),
        "RMI-IIOP IDL fixture must be present");
    for (String scenario : List.of("object-reference", "naming", "giop", "iiop")) {
      assertTrue(
          Files.isRegularFile(repoRoot().resolve("interop/idl/" + scenario + ".idl")),
          scenario + " IDL fixture must be present for live matrix mounting");
    }
    assertTrue(
        Files.isRegularFile(repoRoot().resolve("interop/idl/time-service.idl")),
        "Time Service IDL fixture must be present for live matrix mounting");
    InteropScenario eventService = InteropScenario.eventService();
    assertEquals("event-service", eventService.name());
    assertEquals("interop/idl/event-service.idl", eventService.idlPath());
    assertTrue(
        Files.isRegularFile(repoRoot().resolve(eventService.idlPath())),
        "Event Service IDL fixture must be present for metadata dry runs");
    InteropScenario notificationService = InteropScenario.notificationService();
    assertEquals("notification-service", notificationService.name());
    assertEquals("interop/idl/notification-service.idl", notificationService.idlPath());
    assertTrue(
        Files.isRegularFile(repoRoot().resolve(notificationService.idlPath())),
        "Notification Service IDL fixture must be present for metadata dry runs");
  }

  @Test
  void g12WideCorpusFixturesAreTrackedAndMappedToLocalScenarioIds() throws Exception {
    for (InteropScenario scenario : InteropFeatureCorpus.g12Wide()) {
      Path fixture = repoRoot().resolve(scenario.idlPath());
      assertTrue(Files.isRegularFile(fixture), scenario.idlPath());
      assertTrue(
          Files.readString(fixture, StandardCharsets.UTF_8).contains("valuetype")
              || Files.readString(fixture, StandardCharsets.UTF_8).contains("union"),
          "broad corpus fixture should exercise richer IDL: " + fixture);
    }
    InteropScenario unsupported = InteropFeatureCorpus.g12UnsupportedCustomValue();
    assertTrue(Files.isRegularFile(repoRoot().resolve(unsupported.idlPath())));

    String harness = Files.readString(repoRoot().resolve("interop/bin/interop-peer"));
    for (InteropScenario scenario : InteropFeatureCorpus.g12Wide()) {
      assertTrue(harness.contains(scenario.name()), scenario.name());
      assertTrue(harness.contains(scenario.idlPath()), scenario.idlPath());
    }
  }

  @Test
  void actualPeerManifestsDeclareG12WideCoreScenarioCapabilities() throws Exception {
    for (String peer : REAL_PEERS) {
      String manifest =
          Files.readString(repoRoot().resolve("interop/peers/" + peer + "/peer.yaml"));

      assertTrue(manifest.contains("  - g12-wide-core-types"), manifest);
      assertTrue(manifest.contains("scenarioCapabilities:"), manifest);
      assertTrue(manifest.contains("g12-wide-core-types:"), manifest);
      assertTrue(manifest.contains("idl: interop/idl/g12-wide/CoreTypes.idl"), manifest);
      assertTrue(manifest.contains("support: live-mounted-idl-object-reference-smoke"), manifest);
      assertTrue(manifest.contains("peer-server-to-local-client"), manifest);
      assertTrue(manifest.contains("local-server-to-peer-client"), manifest);
    }
  }

  @Test
  void g12WideCoreScenarioMissingCacheWritesStructuredPeerReport() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("launch", FIXTURE_PEER, "server", "g12-wide-core-types"),
            fixture.environment());

    assertFailure(result, "g12-wide-core-types-server.json");
    String report =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/g12-wide-core-types-server.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"scenario\": \"g12-wide-core-types\""), report);
    assertTrue(report.contains("\"idl\": \"interop/idl/g12-wide/CoreTypes.idl\""), report);
    assertTrue(report.contains("\"classification\": \"infrastructure-failure\""), report);
  }

  @Test
  void runScenarioAllFiltersPeersByDeclaredG12Capability() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path otherPeer = fixture.root().resolve("interop/peers/other-peer");
    Files.createDirectories(otherPeer);
    Files.writeString(
        otherPeer.resolve("peer.yaml"),
        removeG12CoreCapability(
            peerManifest(FixtureOptions.valid()).replace("fixture-peer", "other-peer")),
        StandardCharsets.UTF_8);

    CommandResult result =
        run(
            command("run-scenario", "--dry-run", "g12-wide-core-types", "all"),
            fixture.environment());

    assertSuccess(result);
    assertTrue(
        result
            .output()
            .contains(
                "dry-run: would run scenario g12-wide-core-types role server for fixture-peer"),
        result.output());
    assertFalse(result.output().contains("for other-peer"), result.output());
  }

  @Test
  void runScenarioAllFiltersPeersByDeclaredEventServiceCapability() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path otherPeer = fixture.root().resolve("interop/peers/other-peer");
    Files.createDirectories(otherPeer);
    Files.writeString(
        otherPeer.resolve("peer.yaml"),
        removeEventServiceCapability(
            peerManifest(FixtureOptions.valid()).replace("fixture-peer", "other-peer")),
        StandardCharsets.UTF_8);

    CommandResult result =
        run(command("run-scenario", "--dry-run", "event-service", "all"), fixture.environment());

    assertSuccess(result);
    assertTrue(
        result
            .output()
            .contains("dry-run: would run scenario event-service role server for " + FIXTURE_PEER),
        result.output());
    assertFalse(result.output().contains("for other-peer"), result.output());
  }

  @Test
  void runScenarioAllFiltersPeersByDeclaredNotificationServiceCapability() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path otherPeer = fixture.root().resolve("interop/peers/other-peer");
    Files.createDirectories(otherPeer);
    Files.writeString(
        otherPeer.resolve("peer.yaml"),
        removeNotificationServiceCapability(
            peerManifest(FixtureOptions.valid()).replace("fixture-peer", "other-peer")),
        StandardCharsets.UTF_8);

    CommandResult result =
        run(
            command("run-scenario", "--dry-run", "notification-service", "all"),
            fixture.environment());

    assertSuccess(result);
    assertTrue(
        result
            .output()
            .contains(
                "dry-run: would run scenario notification-service role server for " + FIXTURE_PEER),
        result.output());
    assertFalse(result.output().contains("for other-peer"), result.output());
  }

  @Test
  void manifestValidationRequiresCapabilityMetadataForEveryScenarioGroup() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path manifest = fixture.root().resolve("interop/peers/fixture-peer/peer.yaml");
    Files.writeString(
        manifest,
        removeG12CoreCapability(Files.readString(manifest, StandardCharsets.UTF_8)),
        StandardCharsets.UTF_8);

    CommandResult result = run(command("validate-manifests"), fixture.environment());

    assertFailure(
        result, "scenarioCapabilities must describe every scenarioGroup: g12-wide-core-types");
  }

  @Test
  void directionMatrixDryRunHonorsDeclaredG12CapabilityDirectionsAndRuntimes() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path manifest = fixture.root().resolve("interop/peers/fixture-peer/peer.yaml");
    Files.writeString(
        manifest,
        replaceG12CoreCapability(
            Files.readString(manifest, StandardCharsets.UTF_8),
            """
              g12-wide-core-types:
                idl: interop/idl/g12-wide/CoreTypes.idl
                support: live-mounted-idl-object-reference-smoke
                directions:
                  - peer-server-to-local-client
                localRuntimes:
                  - jvm
                expectedClassifications:
                  - expected-deferral
            """),
        StandardCharsets.UTF_8);

    CommandResult result =
        run(
            command("run-direction-matrix", "--dry-run", "g12-wide-core-types", FIXTURE_PEER),
            fixture.environment());

    assertSuccess(result);
    assertTrue(
        result.output().contains("dry-run: would start fixture-peer server"), result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_JVM_CLIENT_COMMAND"),
        result.output());
    assertFalse(result.output().contains("MJO_NATIVE_CLIENT_BINARY"), result.output());
    assertFalse(result.output().contains("start our jvm server"), result.output());
    assertFalse(result.output().contains("start our native server"), result.output());
  }

  @Test
  void g12WideCoreScenarioReportsAreIncludedInSummaryAggregation() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Map<String, String> environment =
        fixture.environmentWithCache(
            Map.of(
                "CONTAINER_RUNTIME",
                "/bin/true",
                "INTEROP_JAVA_BASE_IMAGE",
                DIGEST_PINNED_BASE_IMAGE));
    run(command("launch", FIXTURE_PEER, "server", "g12-wide-core-types"), environment);

    CommandResult result = run(command("report", FIXTURE_PEER), environment);

    assertSuccess(result);
    String summary =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/summary.json"),
            StandardCharsets.UTF_8);
    assertTrue(summary.contains("\"g12-wide-core-types\": 1"), summary);
    assertTrue(summary.contains("g12-wide-core-types-server.json"), summary);
  }

  @Test
  void g12WideLocalJvmLaneWritesStructuredReportsForSelectedFixture() throws Exception {
    Path root = temporaryDirectory.resolve("g12-wide-local-root");
    Path fixture = root.resolve("interop/idl/g12-wide/ValueTypes.idl");
    Files.createDirectories(Objects.requireNonNull(fixture.getParent()));
    Files.copy(repoRoot().resolve("interop/idl/g12-wide/ValueTypes.idl"), fixture);

    CommandResult result =
        run(
            command(
                "local-lane-report",
                "g12-wide-valuetypes",
                "jvm",
                "client",
                "local",
                "local-corpus"),
            Map.of(
                "INTEROP_ROOT",
                root.toString(),
                "MJO_JVM_CLIENT_COMMAND",
                "test -f \"$MJO_INTEROP_IDL\""));

    assertSuccess(result);
    String report =
        Files.readString(
            root.resolve(
                "build/interop/local/reports/"
                    + "g12-wide-valuetypes-local-jvm-client-local-corpus.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"scenario\": \"g12-wide-valuetypes\""), report);
    assertTrue(report.contains("\"idl\": \"interop/idl/g12-wide/ValueTypes.idl\""), report);
    assertTrue(report.contains("\"status\": \"passed\""), report);
    assertTrue(report.contains("\"classification\": \"expected-deferral\""), report);
  }

  @Test
  void javaPeerSmokeUsesStableEndpointAndCalculatorScenarioBehavior() throws Exception {
    String smoke =
        Files.readString(
            repoRoot().resolve("interop/container/java-peer-smoke/PeerSmoke.java"),
            StandardCharsets.UTF_8);

    assertTrue(smoke.contains("CALCULATOR_REPOSITORY_ID"), smoke);
    assertTrue(smoke.contains("CalculatorServant"), smoke);
    assertTrue(smoke.contains("request.operation()"), smoke);
    assertTrue(smoke.contains("insert_wstring(\"Calculator \" + value)"), smoke);
    assertTrue(smoke.contains("PROBLEM_REPOSITORY_ID"), smoke);
    assertTrue(
        smoke.contains("problem.read_value(out.create_input_stream(), problemType())"), smoke);
    assertTrue(smoke.contains("OAPort"), smoke);
    assertTrue(smoke.contains("jacorb.ior_proxy_host"), smoke);
    assertTrue(smoke.contains("com.sun.CORBA.ORBServerHost"), smoke);
    assertTrue(smoke.contains("com.sun.CORBA.transport.ORBListenSocket"), smoke);
    assertTrue(smoke.contains("!\"jboss-openjdk-orb\".equals(peer())"), smoke);
  }

  @Test
  void peerServerIorCopyDoesNotTruncateMountedIor() throws Exception {
    String harness =
        Files.readString(repoRoot().resolve("interop/bin/interop-peer"), StandardCharsets.UTF_8);

    assertTrue(harness.contains("copied_ior=\"${server_ior}.copy.$$\""), harness);
    assertTrue(harness.contains("mv \"${copied_ior}\" \"${server_ior}\""), harness);
    assertFalse(harness.contains(">\"${server_ior}\" 2>/dev/null"), harness);
  }

  @Test
  void aceTaoUsesPeerSpecificImageAndCleanRoomCommandSources() throws Exception {
    Path peerRoot = repoRoot().resolve("interop/peers/ace-tao");
    String manifest = Files.readString(peerRoot.resolve("peer.yaml"), StandardCharsets.UTF_8);
    String containerfile =
        Files.readString(peerRoot.resolve("Containerfile"), StandardCharsets.UTF_8);
    String peerEntry =
        Files.readString(peerRoot.resolve("peer/peer-entry.sh"), StandardCharsets.UTF_8);

    assertTrue(manifest.contains("buildStatus: peer-specific-containerfile"), manifest);
    assertTrue(manifest.contains("buildFile: interop/peers/ace-tao/Containerfile"), manifest);
    assertTrue(containerfile.contains("ACE+TAO-8.0.6.tar.bz2"), containerfile);
    assertTrue(containerfile.contains("make -C \"${ACE_ROOT}\""), containerfile);
    assertTrue(containerfile.contains("interop/idl/rmi-iiop/Calculator.idl"), containerfile);
    assertTrue(
        containerfile.contains("tao_idl -o /interop/peer/generated/rmi-iiop"), containerfile);
    assertTrue(containerfile.contains("ace_tao_peer.cpp"), containerfile);
    assertTrue(containerfile.contains("CalculatorC.cpp"), containerfile);
    assertTrue(containerfile.contains("CalculatorS.cpp"), containerfile);
    assertTrue(peerEntry.contains("unsupported-scenario"), peerEntry);
    assertFalse(peerEntry.contains("does not implement the Java RMI-IIOP Calculator lane"));
    assertFalse(containerfile.contains("curl "), containerfile);
    assertFalse(containerfile.contains("wget "), containerfile);
    assertFalse(containerfile.contains("git clone"), containerfile);
  }

  @Test
  void aceTaoPeerCommandContractIsTrackedAndDoesNotVendorArtifacts() throws Exception {
    Path peerRoot = repoRoot().resolve("interop/peers/ace-tao/peer");

    for (String command : List.of("client", "server", "naming", "health", "report")) {
      Path script = peerRoot.resolve(command + ".sh");
      assertTrue(Files.isRegularFile(script), command + " command must be tracked");
      String content = Files.readString(script, StandardCharsets.UTF_8);
      assertTrue(content.contains("peer-entry.sh"), content);
    }
    String cpp = Files.readString(peerRoot.resolve("src/ace_tao_peer.cpp"), StandardCharsets.UTF_8);
    String peerEntry = Files.readString(peerRoot.resolve("peer-entry.sh"), StandardCharsets.UTF_8);
    assertTrue(cpp.contains("CORBA::ORB_init"), cpp);
    assertTrue(cpp.contains("RootPOA"), cpp);
    assertTrue(cpp.contains("\"_non_existent\""), cpp);
    assertTrue(cpp.contains("object->_non_existent()"), cpp);
    assertTrue(cpp.contains("CalculatorServant"), cpp);
    assertTrue(cpp.contains("calculator->add(13, 29)"), cpp);
    assertTrue(cpp.contains("calculator->describe(L\"Ada\")"), cpp);
    assertTrue(cpp.contains("example::calc::CalculatorProblem"), cpp);
    assertTrue(
        peerEntry.contains("Standalone ACE/TAO health requires a running scenario server"),
        peerEntry);
    assertFalse(cpp.contains("ACE_TAO_PLACEHOLDER"), cpp);
    assertFalse(cpp.contains("system("), cpp);
  }

  @Test
  void aceTaoDoesNotCommitGeneratedCalculatorBindings() throws Exception {
    Path peerRoot = repoRoot().resolve("interop/peers/ace-tao");

    assertFalse(Files.exists(peerRoot.resolve("peer/generated")));
    assertFalse(Files.exists(peerRoot.resolve("peer/CalculatorC.cpp")));
    assertFalse(Files.exists(peerRoot.resolve("peer/CalculatorS.cpp")));
  }

  @Test
  void approvedFixtureValidatesWithRequiredCache() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(command("validate-gates", "--require-cache"), fixture.environmentWithCache());

    assertSuccess(result);
    assertTrue(
        result.output().contains(FIXTURE_PEER + ": interop/approvals/fixture-peer.approval.yaml"));
  }

  @Test
  void requestedUnknownPeerFailsBeforeCacheOrLiveActions() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(command("validate-gates", "--require-cache", "missing-peer"), fixture.environment());

    assertFailure(result, "unknown peer");
  }

  @Test
  void emptyScenarioGroupsAreRejectedByManifestValidation() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path manifest = fixture.root().resolve("interop/peers/fixture-peer/peer.yaml");
    Files.writeString(
        manifest,
        Files.readString(manifest, StandardCharsets.UTF_8)
            .replace(
                """
                  - basic-idl
                  - rmi-iiop
                  - time-service
                  - event-service
                  - notification-service
                  - g12-wide-core-types
                  - g13-durable-ior-peer-client-restart
                  - g13-durable-naming-peer-client-restart
                """,
                ""),
        StandardCharsets.UTF_8);

    CommandResult result = run(command("validate-manifests"), fixture.environment());

    assertFailure(result, "scenarioGroups must not be empty");
  }

  @Test
  void requiredCacheRejectsMissingEntry() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Files.delete(fixture.cacheRoot().resolve(FIXTURE_CACHE_ENTRY));

    CommandResult result =
        run(command("validate-gates", "--require-cache"), fixture.environmentWithCache());

    assertFailure(result, "cache entry is missing");
  }

  @Test
  void requiredCacheRejectsChecksumMismatch() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Files.writeString(
        fixture.cacheRoot().resolve(FIXTURE_CACHE_ENTRY), "changed\n", StandardCharsets.UTF_8);

    CommandResult result =
        run(command("validate-gates", "--require-cache"), fixture.environmentWithCache());

    assertFailure(result, "cache entry checksum mismatch");
  }

  @Test
  void pendingApprovalFailsDeterministically() throws Exception {
    Fixture fixture =
        createFixture(FixtureOptions.valid().withApprovalStatus("pending-human-review"));

    CommandResult result = run(command("validate-gates"), fixture.environment());

    assertFailure(result, "approvalStatus must be approved-for-black-box-interop");
  }

  @Test
  void mismatchedApprovalPeerFailsDeterministically() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid().withApprovalPeer("other-peer"));

    CommandResult result = run(command("validate-gates"), fixture.environment());

    assertFailure(result, "approval peer must be fixture-peer");
  }

  @Test
  void vendoredPeerArtifactsAreRejectedByManifestValidation() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid().withVendoredPeerArtifacts(true));

    CommandResult result = run(command("validate-manifests"), fixture.environment());

    assertFailure(result, "vendoredPeerArtifacts must be false");
  }

  @Test
  void nonDigestBaseImageIsRejectedBeforeRealBuild() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("build-image", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME", "/bin/true", "INTEROP_JAVA_BASE_IMAGE", "ubuntu:22.04")));

    assertFailure(result, "INTEROP_JAVA_BASE_IMAGE must be set to a digest-pinned image");
  }

  @Test
  void realBuildPreparationValidatesOnlyRequestedPeerCache() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path otherPeer = fixture.root().resolve("interop/peers/other-peer");
    Files.createDirectories(otherPeer);
    Files.writeString(
        otherPeer.resolve("peer.yaml"),
        peerManifest(FixtureOptions.valid())
            .replace("fixture-peer", "other-peer")
            .replace("example:fixture:1.0", "example:other:1.0")
            .replace(
                "maven/example/fixture/1.0/fixture-1.0.jar",
                "maven/example/other/1.0/other-1.0.jar"),
        StandardCharsets.UTF_8);
    Files.writeString(
        fixture.root().resolve("interop/approvals/other-peer.approval.yaml"),
        approval(FixtureOptions.valid())
            .replace("fixture-peer", "other-peer")
            .replace("example:fixture:1.0", "example:other:1.0")
            .replace(
                "maven/example/fixture/1.0/fixture-1.0.jar",
                "maven/example/other/1.0/other-1.0.jar"),
        StandardCharsets.UTF_8);

    CommandResult result =
        run(
            command("build-image", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    "/bin/true",
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertSuccess(result);
  }

  @Test
  void dryRunCommandsRemainAvailableForEveryApprovedPeer() throws Exception {
    for (String peer : REAL_PEERS) {
      assertSuccess(run(command("build-image", "--dry-run", peer), Map.of()));
      assertSuccess(run(command("prepare-cache", "--dry-run", peer), Map.of()));
      assertSuccess(run(command("launch", "--dry-run", peer, "server"), Map.of()));
      assertSuccess(run(command("health", "--dry-run", peer), Map.of()));
      assertSuccess(run(command("report", "--dry-run", peer), Map.of()));
    }
  }

  @Test
  void launchWritesPrerequisiteFailureReportWhenCacheIsMissing() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(command("launch", FIXTURE_PEER, "server", "basic-idl"), fixture.environment());

    assertFailure(result, "basic-idl-server.json");
    String report =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/basic-idl-server.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"status\": \"failed\""), report);
    assertTrue(report.contains("\"classification\": \"infrastructure-failure\""), report);
    assertTrue(
        report.contains(
            "\"stderrPath\": \"build/interop/fixture/logs/basic-idl-server.stderr.log\""),
        report);
  }

  @Test
  void rmiIiopLaunchWritesPrerequisiteFailureReportWhenCacheIsMissing() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(command("launch", FIXTURE_PEER, "server", "rmi-iiop"), fixture.environment());

    assertFailure(result, "rmi-iiop-server.json");
    String report =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/rmi-iiop-server.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"scenario\": \"rmi-iiop\""), report);
    assertTrue(report.contains("\"idl\": \"interop/idl/rmi-iiop/Calculator.idl\""), report);
    assertTrue(report.contains("\"classification\": \"infrastructure-failure\""), report);
  }

  @Test
  void launchRunsConfiguredContainerCommandWhenPrerequisitesExist() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("launch", FIXTURE_PEER, "server", "basic-idl"),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    "/bin/true",
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertSuccess(result);
    String report =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/basic-idl-server.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"status\": \"passed\""), report);
    assertTrue(report.contains("\"classification\": \"expected-deferral\""), report);
    assertTrue(report.contains("\"command\": \"server\""), report);
  }

  @Test
  void launchUsesExplicitContainerNetworkWhenConfigured() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtimeLog = temporaryDirectory.resolve("runtime-network.log");
    Path runtime = fakeContainerRuntime("networked-runtime", 0, 0);

    CommandResult result =
        run(
            command("launch", FIXTURE_PEER, "client", "basic-idl"),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "FAKE_RUNTIME_LOG",
                    runtimeLog.toString(),
                    "INTEROP_CONTAINER_NETWORK",
                    "mjo-test-network",
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertSuccess(result);
    String runtimeCalls = Files.readString(runtimeLog, StandardCharsets.UTF_8);
    assertTrue(runtimeCalls.contains("network inspect mjo-test-network"), runtimeCalls);
    assertTrue(runtimeCalls.contains("run --rm"), runtimeCalls);
    assertTrue(runtimeCalls.contains("--network mjo-test-network"), runtimeCalls);
    assertTrue(runtimeCalls.contains("--add-host host.docker.internal:host-gateway"), runtimeCalls);
  }

  @Test
  void launchAllowsHostGatewayOverrideForLocalServerReachability() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtimeLog = temporaryDirectory.resolve("runtime-host-gateway.log");
    Path runtime = fakeContainerRuntime("host-gateway-runtime", 0, 0);

    CommandResult result =
        run(
            command("launch", FIXTURE_PEER, "client", "basic-idl"),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "FAKE_RUNTIME_LOG",
                    runtimeLog.toString(),
                    "INTEROP_HOST_GATEWAY_NAME",
                    "mjo.host",
                    "INTEROP_HOST_GATEWAY_VALUE",
                    "172.17.0.1",
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertSuccess(result);
    String runtimeCalls = Files.readString(runtimeLog, StandardCharsets.UTF_8);
    assertTrue(runtimeCalls.contains("--add-host mjo.host:172.17.0.1"), runtimeCalls);
  }

  @Test
  void launchReportsExplicitContainerNetworkSetupFailure() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("network-fails", 0, 0, 42);

    CommandResult result =
        run(
            command("launch", FIXTURE_PEER, "client", "basic-idl"),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "INTEROP_CONTAINER_NETWORK",
                    "mjo-test-network",
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertFailure(result, "basic-idl-client.json");
    String report =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/basic-idl-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("G10-120 container network setup failed"), report);
    assertTrue(report.contains("\"classification\": \"infrastructure-failure\""), report);
  }

  @Test
  void launchWritesPrerequisiteFailureReportWhenBaseImageIsMissing() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("launch", FIXTURE_PEER, "server", "basic-idl"),
            fixture.environmentWithCache(Map.of("CONTAINER_RUNTIME", "/bin/true")));

    assertFailure(result, "basic-idl-server.json");
    String report =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/basic-idl-server.json"),
            StandardCharsets.UTF_8);
    String stderr =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/logs/basic-idl-server.stderr.log"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"classification\": \"infrastructure-failure\""), report);
    assertTrue(report.contains("G6-830 base image validation failed"), report);
    assertTrue(stderr.contains("INTEROP_JAVA_BASE_IMAGE must be set to a digest-pinned image"));
  }

  @Test
  void launchWritesPrerequisiteFailureReportWhenBaseImageIsNotDigestPinned() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("launch", FIXTURE_PEER, "server", "basic-idl"),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME", "/bin/true", "INTEROP_JAVA_BASE_IMAGE", "ubuntu:22.04")));

    assertFailure(result, "basic-idl-server.json");
    String stderr =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/logs/basic-idl-server.stderr.log"),
            StandardCharsets.UTF_8);
    assertTrue(stderr.contains("INTEROP_JAVA_BASE_IMAGE must be set to a digest-pinned image"));
  }

  @Test
  void launchWritesPrerequisiteFailureReportWhenContainerRuntimeIsMissing() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path toolPath = temporaryDirectory.resolve("tool-path");
    Files.createDirectories(toolPath);
    for (String tool : List.of("bash", "python3", "date", "dirname", "mkdir")) {
      Files.createSymbolicLink(toolPath.resolve(tool), executableOnPath(tool));
    }

    CommandResult result =
        run(
            command("launch", FIXTURE_PEER, "server", "basic-idl"),
            fixture.environmentWithCache(
                Map.of(
                    "PATH",
                    toolPath.toString(),
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertFailure(result, "basic-idl-server.json");
    String report =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/basic-idl-server.json"),
            StandardCharsets.UTF_8);
    String stderr =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/logs/basic-idl-server.stderr.log"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("G6-830 container runtime resolution failed"), report);
    assertTrue(stderr.contains("CONTAINER_RUNTIME is unset"), stderr);
  }

  @Test
  void launchWritesPrerequisiteFailureReportWhenPeerImageIsMissing() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("image-missing", 1, 0);

    CommandResult result =
        run(
            command("launch", FIXTURE_PEER, "server", "basic-idl"),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertFailure(result, "basic-idl-server.json");
    String report =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/basic-idl-server.json"),
            StandardCharsets.UTF_8);
    String stderr =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/logs/basic-idl-server.stderr.log"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("G10-110 peer image validation failed"), report);
    assertTrue(stderr.contains("peer image is missing; run build-image first"), stderr);
  }

  @Test
  void launchWritesStructuredFailureReportWhenContainerCommandFails() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("run-fails", 0, 42);

    CommandResult result =
        run(
            command("launch", FIXTURE_PEER, "server", "basic-idl"),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertFailure(result, "basic-idl-server.json");
    String report =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/basic-idl-server.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"status\": \"failed\""), report);
    assertTrue(report.contains("\"classification\": \"infrastructure-failure\""), report);
    assertTrue(report.contains("\"exitCode\": 42"), report);
    assertTrue(report.contains("G10-110 container command failed"), report);
  }

  @Test
  void launchClassifiesExplicitUnsupportedScenarioExit() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("unsupported-scenario", 0, 67);

    CommandResult result =
        run(
            command("launch", FIXTURE_PEER, "client", "rmi-iiop"),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertFailure(result, "rmi-iiop-client.json");
    String report =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/rmi-iiop-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"classification\": \"unsupported-scenario\""), report);
    assertTrue(report.contains("G10-120 peer command reported an unsupported scenario"), report);
  }

  @Test
  void launchClassifiesExplicitMissingPrerequisiteExit() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("missing-prerequisite", 0, 66);

    CommandResult result =
        run(
            command("launch", FIXTURE_PEER, "client", "basic-idl"),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertFailure(result, "basic-idl-client.json");
    String report =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/basic-idl-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"classification\": \"missing-prerequisite\""), report);
    assertTrue(report.contains("G10-120 peer command reported a missing prerequisite"), report);
  }

  @Test
  void runScenarioUsesDetachedServerHealthClientAndCleanup() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtimeLog = temporaryDirectory.resolve("runtime.log");
    Path runtime = fakeContainerRuntime("live-scenario", 0, 0);
    Path stalePeerServerIor =
        fixture.root().resolve("build/interop/fixture/iors/basic-idl-server.ior");
    Path stalePeerServerIorParent = stalePeerServerIor.getParent();
    assertNotNull(stalePeerServerIorParent);
    Files.createDirectories(stalePeerServerIorParent);
    Files.writeString(stalePeerServerIor, "IOR:stale-peer", StandardCharsets.UTF_8);

    CommandResult result =
        run(
            command("run-scenario", "--require-live", "basic-idl", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "FAKE_RUNTIME_LOG",
                    runtimeLog.toString(),
                    "INTEROP_HEALTH_DELAY_SECONDS",
                    "0",
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertSuccess(result);
    String runtimeCalls = Files.readString(runtimeLog, StandardCharsets.UTF_8);
    assertTrue(runtimeCalls.contains("run -d --name mjo-fixture-peer-basic-idl-server-"));
    assertTrue(runtimeCalls.contains("-p 127.0.0.1:2809:2809"), runtimeCalls);
    assertTrue(runtimeCalls.contains("INTEROP_ROLE=health"));
    assertTrue(runtimeCalls.contains("INTEROP_ROLE=client"));
    assertTrue(runtimeCalls.contains("rm -f mjo-fixture-peer-basic-idl-server-"));
    assertEquals(
        "IOR:fresh-peer",
        Files.readString(stalePeerServerIor, StandardCharsets.UTF_8).trim(),
        "detached server startup must replace stale peer-server IOR content");
    assertTrue(
        Files.exists(
            fixture.root().resolve("build/interop/fixture/reports/basic-idl-server.json")));
    assertTrue(
        Files.exists(
            fixture.root().resolve("build/interop/fixture/reports/basic-idl-health.json")));
    assertTrue(
        Files.exists(
            fixture.root().resolve("build/interop/fixture/reports/basic-idl-client.json")));
  }

  @Test
  void reportSummarizesCapturedStructuredReports() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Map<String, String> environment =
        fixture.environmentWithCache(
            Map.of(
                "CONTAINER_RUNTIME",
                "/bin/true",
                "INTEROP_JAVA_BASE_IMAGE",
                DIGEST_PINNED_BASE_IMAGE));
    run(command("launch", FIXTURE_PEER, "server", "basic-idl"), environment);
    run(command("launch", FIXTURE_PEER, "server", "rmi-iiop"), environment);

    CommandResult result = run(command("report", FIXTURE_PEER), environment);

    assertSuccess(result);
    String summary =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/summary.json"),
            StandardCharsets.UTF_8);
    assertTrue(summary.contains("\"peer\": \"fixture-peer\""), summary);
    assertTrue(summary.contains("\"reportCount\": 3"), summary);
    assertTrue(summary.contains("\"classificationCounts\""), summary);
    assertTrue(summary.contains("\"expected-deferral\": 3"), summary);
    assertTrue(summary.contains("\"statusCounts\""), summary);
    assertTrue(summary.contains("\"passed\": 3"), summary);
    assertTrue(summary.contains("\"scenarioCounts\""), summary);
    assertTrue(summary.contains("\"failures\": []"), summary);
    assertTrue(summary.contains("basic-idl-server.json"), summary);
    assertTrue(summary.contains("rmi-iiop-server.json"), summary);
    assertTrue(summary.contains("report-report.json"), summary);
  }

  @Test
  void reportSummaryIgnoresPeerSideAuxiliaryJson() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Map<String, String> environment =
        fixture.environmentWithCache(
            Map.of(
                "CONTAINER_RUNTIME",
                "/bin/true",
                "INTEROP_JAVA_BASE_IMAGE",
                DIGEST_PINNED_BASE_IMAGE));
    run(command("launch", FIXTURE_PEER, "server", "basic-idl"), environment);
    Path auxiliaryReport =
        fixture.root().resolve("build/interop/fixture/reports/basic-idl-server.peer.json");
    Files.writeString(
        auxiliaryReport,
        """
        {
          "peer": "fixture-peer",
          "status": "passed"
        }
        """,
        StandardCharsets.UTF_8);

    CommandResult result = run(command("report", FIXTURE_PEER), environment);

    assertSuccess(result);
    String summary =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/summary.json"),
            StandardCharsets.UTF_8);
    assertTrue(summary.contains("\"reportCount\": 2"), summary);
    assertFalse(summary.contains("basic-idl-server.peer.json"), summary);
  }

  @Test
  void runScenarioDryRunRemainsNonMutating() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(command("run-scenario", "--dry-run", "rmi-iiop", FIXTURE_PEER), fixture.environment());

    assertSuccess(result);
    assertTrue(
        result
            .output()
            .contains("dry-run: would run scenario rmi-iiop role server for fixture-peer"));
    assertTrue(
        result
            .output()
            .contains("dry-run: would run scenario rmi-iiop role client for fixture-peer"));
    assertFalse(Files.exists(fixture.root().resolve("build/interop/fixture/reports")));
  }

  @Test
  void directionMatrixDryRunEnumeratesLocalAndPeerDirectionsWithoutMutating() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-direction-matrix", "--dry-run", "basic-idl", FIXTURE_PEER),
            fixture.environment());

    assertSuccess(result);
    assertTrue(
        result.output().contains("dry-run: would validate MJO_JVM_CLIENT_COMMAND"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_JVM_SERVER_COMMAND"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_NATIVE_CLIENT_BINARY"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_NATIVE_SERVER_BINARY"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would start fixture-peer server for basic-idl"),
        result.output());
    assertTrue(
        result
            .output()
            .contains(
                "dry-run: would run fixture-peer client against our jvm server for basic-idl"),
        result.output());
    assertFalse(Files.exists(fixture.root().resolve("build/interop/local/reports")));
    assertFalse(Files.exists(fixture.root().resolve("build/interop/fixture/reports")));
  }

  @Test
  void timeServiceDirectionMatrixDryRunEnumeratesBothDirectionsAndRuntimesWithoutMutating()
      throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-direction-matrix", "--dry-run", "time-service", FIXTURE_PEER),
            fixture.environment());

    assertSuccess(result);
    assertTrue(
        result.output().contains("dry-run: would start fixture-peer server for time-service"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_JVM_CLIENT_COMMAND"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_NATIVE_CLIENT_BINARY"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_JVM_SERVER_COMMAND"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_NATIVE_SERVER_BINARY"),
        result.output());
    assertTrue(
        result
            .output()
            .contains(
                "dry-run: would run fixture-peer client against our jvm server for time-service"),
        result.output());
    assertTrue(
        result
            .output()
            .contains(
                "dry-run: would run fixture-peer client against our native server for time-service"),
        result.output());
    assertFalse(Files.exists(fixture.root().resolve("build/interop/local/reports")));
    assertFalse(Files.exists(fixture.root().resolve("build/interop/fixture/reports")));
  }

  @Test
  void eventServiceDirectionMatrixDryRunEnumeratesBothDirectionsAndRuntimesWithoutMutating()
      throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-direction-matrix", "--dry-run", "event-service", FIXTURE_PEER),
            fixture.environment());

    assertSuccess(result);
    assertTrue(
        result.output().contains("dry-run: would start fixture-peer server for event-service"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_JVM_CLIENT_COMMAND"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_NATIVE_CLIENT_BINARY"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_JVM_SERVER_COMMAND"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_NATIVE_SERVER_BINARY"),
        result.output());
    assertTrue(
        result
            .output()
            .contains(
                "dry-run: would run fixture-peer client against our jvm server for event-service"),
        result.output());
    assertTrue(
        result
            .output()
            .contains(
                "dry-run: would run fixture-peer client against our native server for "
                    + "event-service"),
        result.output());
    assertFalse(Files.exists(fixture.root().resolve("build/interop/local/reports")));
    assertFalse(Files.exists(fixture.root().resolve("build/interop/fixture/reports")));
  }

  @Test
  void notificationServiceDirectionMatrixDryRunEnumeratesBothDirectionsAndRuntimesWithoutMutating()
      throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-direction-matrix", "--dry-run", "notification-service", FIXTURE_PEER),
            fixture.environment());

    assertSuccess(result);
    assertTrue(
        result
            .output()
            .contains("dry-run: would start fixture-peer server for notification-service"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_JVM_CLIENT_COMMAND"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_NATIVE_CLIENT_BINARY"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_JVM_SERVER_COMMAND"),
        result.output());
    assertTrue(
        result.output().contains("dry-run: would validate MJO_NATIVE_SERVER_BINARY"),
        result.output());
    assertTrue(
        result
            .output()
            .contains(
                "dry-run: would run fixture-peer client against our jvm server for "
                    + "notification-service"),
        result.output());
    assertTrue(
        result
            .output()
            .contains(
                "dry-run: would run fixture-peer client against our native server for "
                    + "notification-service"),
        result.output());
    assertFalse(Files.exists(fixture.root().resolve("build/interop/local/reports")));
    assertFalse(Files.exists(fixture.root().resolve("build/interop/fixture/reports")));
  }

  @Test
  void durablePeerRestartDryRunEnumeratesOnlyLocalServerToPeerClientDirections() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command(
                "run-direction-matrix",
                "--dry-run",
                "g13-durable-ior-peer-client-restart",
                FIXTURE_PEER),
            fixture.environment());

    assertSuccess(result);
    assertFalse(
        result.output().contains("would start fixture-peer server"),
        "durable peer scenarios must not claim peer servers emit MJO durable keys");
    assertTrue(
        result
            .output()
            .contains(
                "dry-run: would run fixture-peer client against our jvm server for "
                    + "g13-durable-ior-peer-client-restart"),
        result.output());
    assertTrue(
        result
            .output()
            .contains(
                "dry-run: would run fixture-peer client against our native server for "
                    + "g13-durable-ior-peer-client-restart"),
        result.output());
    assertFalse(Files.exists(fixture.root().resolve("build/interop/local/reports")));
    assertFalse(Files.exists(fixture.root().resolve("build/interop/fixture/reports")));
  }

  @Test
  void durablePeerRestartLiveRunRequiresExplicitApprovalReport() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command(
                "run-direction-matrix",
                "--require-live",
                "g13-durable-naming-peer-client-restart",
                FIXTURE_PEER),
            fixture.environmentWithCache());

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/local/reports/g13-durable-naming-peer-client-restart-"
                        + "fixture-peer-jvm-server-local-server-to-peer-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"classification\": \"missing-prerequisite\""), report);
    assertTrue(report.contains("\"missingPrerequisite\": \"live-approval\""), report);
    assertTrue(report.contains("\"liveApprovalState\": \"missing\""), report);
    assertTrue(report.contains("\"direction\": \"local-server-to-peer-client\""), report);
    assertTrue(report.contains("\"localServerRuntime\": \"our-jvm-jdk21\""), report);
    assertTrue(report.contains("\"peerClientRuntime\": \"peer-jvm\""), report);
    assertTrue(report.contains("\"evidencePolicy\": \"clean-room-summary-only\""), report);
  }

  @Test
  void durablePeerRestartPrerequisiteReportNamesMissingServerCommand() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command(
                "run-direction-matrix",
                "--require-live",
                "g13-durable-ior-peer-client-restart",
                FIXTURE_PEER),
            fixture.environmentWithCache(Map.of("INTEROP_DURABLE_LIVE_APPROVED", "true")));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/local/reports/g13-durable-ior-peer-client-restart-"
                        + "fixture-peer-jvm-server-local-server-to-peer-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"MJO_JVM_SERVER_COMMAND\""), report);
    assertTrue(report.contains("\"liveApprovalState\": \"true\""), report);
    assertTrue(report.contains("\"expectedClassification\": \"durable-ior-invoked\""), report);
  }

  @Test
  void durablePeerRestartPrerequisiteReportNamesMissingArtifactCache() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Files.delete(fixture.cacheRoot().resolve(FIXTURE_CACHE_ENTRY));

    CommandResult result =
        run(
            command(
                "run-direction-matrix",
                "--require-live",
                "g13-durable-ior-peer-client-restart",
                FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "INTEROP_DURABLE_LIVE_APPROVED",
                    "true",
                    "MJO_JVM_SERVER_COMMAND",
                    "/bin/true")));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/local/reports/g13-durable-ior-peer-client-restart-"
                        + "fixture-peer-jvm-server-local-server-to-peer-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"artifact-cache\""), report);
    assertTrue(report.contains("cache entry is missing"), report);
  }

  @Test
  void durablePeerRestartPrerequisiteReportNamesMissingNativeBinary() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command(
                "run-direction-matrix",
                "--require-live",
                "g13-durable-ior-peer-client-restart",
                FIXTURE_PEER),
            fixture.environmentWithCache(Map.of("INTEROP_DURABLE_LIVE_APPROVED", "true")));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/local/reports/g13-durable-ior-peer-client-restart-"
                        + "fixture-peer-native-server-local-server-to-peer-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"MJO_NATIVE_SERVER_BINARY\""), report);
  }

  @Test
  void durablePeerRestartPrerequisiteReportNamesMissingDigestPinnedBaseImage() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command(
                "run-direction-matrix",
                "--require-live",
                "g13-durable-ior-peer-client-restart",
                FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "INTEROP_DURABLE_LIVE_APPROVED",
                    "true",
                    "MJO_JVM_SERVER_COMMAND",
                    "/bin/true")));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/local/reports/g13-durable-ior-peer-client-restart-"
                        + "fixture-peer-jvm-server-local-server-to-peer-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"INTEROP_JAVA_BASE_IMAGE\""), report);
  }

  @Test
  void durablePeerRestartPrerequisiteReportNamesMissingContainerRuntime() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path isolatedBin = isolatedPathWithoutContainerRuntime();

    CommandResult result =
        run(
            command(
                "run-direction-matrix",
                "--require-live",
                "g13-durable-ior-peer-client-restart",
                FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "PATH",
                    isolatedBin.toString(),
                    "INTEROP_DURABLE_LIVE_APPROVED",
                    "true",
                    "MJO_JVM_SERVER_COMMAND",
                    "/bin/true",
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/local/reports/g13-durable-ior-peer-client-restart-"
                        + "fixture-peer-jvm-server-local-server-to-peer-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"container-runtime\""), report);
  }

  @Test
  void durablePeerRestartPrerequisiteReportNamesMissingPeerImage() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("durable-missing-image", 1, 0);

    CommandResult result =
        run(
            command(
                "run-direction-matrix",
                "--require-live",
                "g13-durable-ior-peer-client-restart",
                FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "INTEROP_DURABLE_LIVE_APPROVED",
                    "true",
                    "MJO_JVM_SERVER_COMMAND",
                    "/bin/true",
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/local/reports/g13-durable-ior-peer-client-restart-"
                        + "fixture-peer-jvm-server-local-server-to-peer-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"peer-image\""), report);
  }

  @Test
  void timeServiceLiveRunRequiresExplicitApprovalReport() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "time-service", FIXTURE_PEER),
            fixture.environmentWithCache());

    assertEquals(1, result.exitCode(), result.output());
    assertFalse(
        Files.exists(fixture.root().resolve("build/interop/fixture/reports")),
        "G8-120 must not start peer-server live work before G8-140 implements the live lane");
    String report =
        Files.readString(timeServicePrerequisiteReport(fixture, "jvm"), StandardCharsets.UTF_8);
    assertTrue(report.contains("\"classification\": \"missing-prerequisite\""), report);
    assertTrue(report.contains("\"missingPrerequisite\": \"live-approval\""), report);
    assertTrue(report.contains("\"liveApprovalState\": \"missing\""), report);
    assertTrue(report.contains("\"direction\": \"local-server-to-peer-client\""), report);
    assertTrue(report.contains("\"localServerRuntime\": \"our-jvm-jdk21\""), report);
    assertTrue(report.contains("\"peerClientRuntime\": \"peer-jvm\""), report);
    assertTrue(report.contains("\"expectedClassification\": \"time-service-checked\""), report);
    assertTrue(report.contains("\"evidencePolicy\": \"clean-room-summary-only\""), report);
  }

  @Test
  void eventServiceLiveRunRequiresExplicitApprovalReportsWithoutPeerOutputs() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "event-service", FIXTURE_PEER),
            fixture.environmentWithCache());

    assertEquals(1, result.exitCode(), result.output());
    assertFalse(
        Files.exists(fixture.root().resolve("build/interop/fixture/reports")),
        "G8-260 must not start peer-server live work");
    String report =
        Files.readString(
            eventServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"classification\": \"missing-prerequisite\""), report);
    assertTrue(report.contains("\"missingPrerequisite\": \"live-approval\""), report);
    assertTrue(report.contains("\"liveApprovalState\": \"missing\""), report);
    assertTrue(report.contains("\"direction\": \"peer-server-to-local-client\""), report);
    assertTrue(report.contains("\"localRuntime\": \"our-jvm-jdk21\""), report);
    assertTrue(report.contains("\"peerRuntime\": \"peer-jvm\""), report);
    assertTrue(report.contains("\"service\": \"event-service\""), report);
    assertTrue(
        report.contains("\"operationSet\": \"channel-admin,push,pull,try_pull,disconnect\""));
    assertTrue(report.contains("\"evidencePolicy\": \"clean-room-summary-only\""), report);
  }

  @Test
  void eventServicePrerequisiteReportNamesMissingScenarioIdl() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Files.delete(fixture.root().resolve("interop/idl/event-service.idl"));

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "event-service", FIXTURE_PEER),
            fixture.environmentWithCache(Map.of("INTEROP_EVENT_SERVICE_LIVE_APPROVED", "true")));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            eventServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"scenario-idl\""), report);
    assertTrue(report.contains("Event Service scenario IDL is missing"), report);
  }

  @Test
  void eventServicePrerequisiteReportsNameMissingLocalLaneCommands() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "event-service", FIXTURE_PEER),
            fixture.environmentWithCache(Map.of("INTEROP_EVENT_SERVICE_LIVE_APPROVED", "true")));

    assertEquals(1, result.exitCode(), result.output());
    String jvmClient =
        Files.readString(
            eventServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    String nativeClient =
        Files.readString(
            eventServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "native", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    String jvmServer =
        Files.readString(
            eventServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "server", "local-server-to-peer-client"),
            StandardCharsets.UTF_8);
    String nativeServer =
        Files.readString(
            eventServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "native", "server", "local-server-to-peer-client"),
            StandardCharsets.UTF_8);

    assertTrue(jvmClient.contains("\"missingPrerequisite\": \"MJO_JVM_CLIENT_COMMAND\""));
    assertTrue(nativeClient.contains("\"missingPrerequisite\": \"MJO_NATIVE_CLIENT_BINARY\""));
    assertTrue(jvmServer.contains("\"missingPrerequisite\": \"MJO_JVM_SERVER_COMMAND\""));
    assertTrue(nativeServer.contains("\"missingPrerequisite\": \"MJO_NATIVE_SERVER_BINARY\""));
  }

  @Test
  void eventServicePrerequisiteReportNamesMissingArtifactCache() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Files.delete(fixture.cacheRoot().resolve(FIXTURE_CACHE_ENTRY));

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "event-service", FIXTURE_PEER),
            fixture.environmentWithCache(eventServiceApprovedCommandEnvironment()));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            eventServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"artifact-cache\""), report);
    assertTrue(report.contains("cache entry is missing"), report);
  }

  @Test
  void eventServicePrerequisiteReportNamesMissingDigestPinnedBaseImage() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "event-service", FIXTURE_PEER),
            fixture.environmentWithCache(eventServiceApprovedCommandEnvironment()));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            eventServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"INTEROP_JAVA_BASE_IMAGE\""), report);
  }

  @Test
  void eventServicePrerequisiteReportNamesMissingContainerRuntime() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path isolatedBin = isolatedPathWithoutContainerRuntime();
    Map<String, String> environment = eventServiceApprovedCommandEnvironment();
    environment.put("PATH", isolatedBin.toString());
    environment.put("INTEROP_JAVA_BASE_IMAGE", DIGEST_PINNED_BASE_IMAGE);

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "event-service", FIXTURE_PEER),
            fixture.environmentWithCache(environment));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            eventServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"container-runtime\""), report);
  }

  @Test
  void eventServicePrerequisiteReportNamesMissingPeerImage() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("event-service-missing-image", 1, 0);
    Map<String, String> environment = eventServiceApprovedCommandEnvironment();
    environment.put("CONTAINER_RUNTIME", runtime.toString());
    environment.put("INTEROP_JAVA_BASE_IMAGE", DIGEST_PINNED_BASE_IMAGE);

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "event-service", FIXTURE_PEER),
            fixture.environmentWithCache(environment));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            eventServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"peer-image\""), report);
  }

  @Test
  void eventServiceDoesNotExecuteLiveLanesEvenWhenPrerequisitesPass() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("event-service-live-blocked", 0, 0);
    Map<String, String> environment = eventServiceApprovedCommandEnvironment();
    environment.put("CONTAINER_RUNTIME", runtime.toString());
    environment.put("INTEROP_JAVA_BASE_IMAGE", DIGEST_PINNED_BASE_IMAGE);

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "event-service", FIXTURE_PEER),
            fixture.environmentWithCache(environment));

    assertEquals(1, result.exitCode(), result.output());
    assertFalse(Files.exists(fixture.root().resolve("build/interop/fixture/reports")));
    String report =
        Files.readString(
            eventServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"live-execution\""), report);
    assertTrue(report.contains("G8-260 records Event Service prerequisites only"), report);
  }

  @Test
  void notificationServiceLiveRunRequiresExplicitApprovalReportsWithoutPeerOutputs()
      throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "notification-service", FIXTURE_PEER),
            fixture.environmentWithCache());

    assertEquals(1, result.exitCode(), result.output());
    assertFalse(
        Files.exists(fixture.root().resolve("build/interop/fixture/reports")),
        "G8-380 must not start peer-server live work");
    String report =
        Files.readString(
            notificationServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"classification\": \"missing-prerequisite\""), report);
    assertTrue(report.contains("\"missingPrerequisite\": \"live-approval\""), report);
    assertTrue(report.contains("\"liveApprovalState\": \"missing\""), report);
    assertTrue(report.contains("\"direction\": \"peer-server-to-local-client\""), report);
    assertTrue(report.contains("\"localRuntime\": \"our-jvm-jdk21\""), report);
    assertTrue(report.contains("\"peerRuntime\": \"peer-jvm\""), report);
    assertTrue(report.contains("\"service\": \"notification-service\""), report);
    assertTrue(
        report.contains(
            "\"operationSet\": \"structured-channel-admin,structured-push,pull,try_pull,"
                + "filter,qos,disconnect\""));
    assertTrue(report.contains("\"evidencePolicy\": \"clean-room-summary-only\""), report);
  }

  @Test
  void notificationServiceRunScenarioRequireLiveReportsWithoutPeerOutputs() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-scenario", "--require-live", "notification-service", FIXTURE_PEER),
            fixture.environmentWithCache());

    assertEquals(1, result.exitCode(), result.output());
    assertFalse(
        Files.exists(fixture.root().resolve("build/interop/fixture/reports")),
        "G8-380 direct scenario execution must not start peer roles");
    String report =
        Files.readString(
            notificationServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"classification\": \"missing-prerequisite\""), report);
    assertTrue(report.contains("\"missingPrerequisite\": \"live-approval\""), report);
    assertTrue(report.contains("\"service\": \"notification-service\""), report);
  }

  @Test
  void notificationServicePrerequisiteReportNamesMissingScenarioIdl() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Files.delete(fixture.root().resolve("interop/idl/notification-service.idl"));

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "notification-service", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of("INTEROP_NOTIFICATION_SERVICE_LIVE_APPROVED", "true")));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            notificationServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"scenario-idl\""), report);
    assertTrue(report.contains("Notification Service scenario IDL is missing"), report);
  }

  @Test
  void notificationServicePrerequisiteReportsNameMissingLocalLaneCommands() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "notification-service", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of("INTEROP_NOTIFICATION_SERVICE_LIVE_APPROVED", "true")));

    assertEquals(1, result.exitCode(), result.output());
    String jvmClient =
        Files.readString(
            notificationServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    String nativeClient =
        Files.readString(
            notificationServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "native", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    String jvmServer =
        Files.readString(
            notificationServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "server", "local-server-to-peer-client"),
            StandardCharsets.UTF_8);
    String nativeServer =
        Files.readString(
            notificationServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "native", "server", "local-server-to-peer-client"),
            StandardCharsets.UTF_8);

    assertTrue(jvmClient.contains("\"missingPrerequisite\": \"MJO_JVM_CLIENT_COMMAND\""));
    assertTrue(nativeClient.contains("\"missingPrerequisite\": \"MJO_NATIVE_CLIENT_BINARY\""));
    assertTrue(jvmServer.contains("\"missingPrerequisite\": \"MJO_JVM_SERVER_COMMAND\""));
    assertTrue(nativeServer.contains("\"missingPrerequisite\": \"MJO_NATIVE_SERVER_BINARY\""));
  }

  @Test
  void notificationServicePrerequisiteReportNamesMissingArtifactCache() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Files.delete(fixture.cacheRoot().resolve(FIXTURE_CACHE_ENTRY));

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "notification-service", FIXTURE_PEER),
            fixture.environmentWithCache(notificationServiceApprovedCommandEnvironment()));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            notificationServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"artifact-cache\""), report);
    assertTrue(report.contains("cache entry is missing"), report);
  }

  @Test
  void notificationServicePrerequisiteReportNamesMissingDigestPinnedBaseImage() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "notification-service", FIXTURE_PEER),
            fixture.environmentWithCache(notificationServiceApprovedCommandEnvironment()));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            notificationServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"INTEROP_JAVA_BASE_IMAGE\""), report);
  }

  @Test
  void notificationServicePrerequisiteReportNamesMissingContainerRuntime() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path isolatedBin = isolatedPathWithoutContainerRuntime();
    Map<String, String> environment = notificationServiceApprovedCommandEnvironment();
    environment.put("PATH", isolatedBin.toString());
    environment.put("INTEROP_JAVA_BASE_IMAGE", DIGEST_PINNED_BASE_IMAGE);

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "notification-service", FIXTURE_PEER),
            fixture.environmentWithCache(environment));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            notificationServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"container-runtime\""), report);
  }

  @Test
  void notificationServicePrerequisiteReportNamesMissingPeerImage() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("notification-service-missing-image", 1, 0);
    Map<String, String> environment = notificationServiceApprovedCommandEnvironment();
    environment.put("CONTAINER_RUNTIME", runtime.toString());
    environment.put("INTEROP_JAVA_BASE_IMAGE", DIGEST_PINNED_BASE_IMAGE);

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "notification-service", FIXTURE_PEER),
            fixture.environmentWithCache(environment));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            notificationServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"peer-image\""), report);
  }

  @Test
  void notificationServiceDoesNotExecuteLiveLanesEvenWhenPrerequisitesPass() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("notification-service-live-blocked", 0, 0);
    Map<String, String> environment = notificationServiceApprovedCommandEnvironment();
    environment.put("CONTAINER_RUNTIME", runtime.toString());
    environment.put("INTEROP_JAVA_BASE_IMAGE", DIGEST_PINNED_BASE_IMAGE);

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "notification-service", FIXTURE_PEER),
            fixture.environmentWithCache(environment));

    assertEquals(1, result.exitCode(), result.output());
    assertFalse(Files.exists(fixture.root().resolve("build/interop/fixture/reports")));
    String report =
        Files.readString(
            notificationServicePrerequisiteReport(
                fixture, FIXTURE_PEER, "jvm", "client", "peer-server-to-local-client"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"live-execution\""), report);
    assertTrue(report.contains("G8-380 records Notification Service prerequisites only"), report);
  }

  @Test
  void timeServicePrerequisiteReportNamesMissingScenarioIdl() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Files.delete(fixture.root().resolve("interop/idl/time-service.idl"));

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "time-service", FIXTURE_PEER),
            fixture.environmentWithCache(Map.of("INTEROP_TIME_SERVICE_LIVE_APPROVED", "true")));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(timeServicePrerequisiteReport(fixture, "jvm"), StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"scenario-idl\""), report);
    assertTrue(report.contains("Time Service scenario IDL is missing"), report);
  }

  @Test
  void timeServicePrerequisiteReportNamesMissingServerCommand() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "time-service", FIXTURE_PEER),
            fixture.environmentWithCache(Map.of("INTEROP_TIME_SERVICE_LIVE_APPROVED", "true")));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(timeServicePrerequisiteReport(fixture, "jvm"), StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"MJO_JVM_SERVER_COMMAND\""), report);
    assertTrue(report.contains("\"liveApprovalState\": \"true\""), report);
  }

  @Test
  void timeServicePrerequisiteReportNamesMissingNativeBinary() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "time-service", FIXTURE_PEER),
            fixture.environmentWithCache(Map.of("INTEROP_TIME_SERVICE_LIVE_APPROVED", "true")));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(timeServicePrerequisiteReport(fixture, "native"), StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"MJO_NATIVE_SERVER_BINARY\""), report);
  }

  @Test
  void timeServicePrerequisiteReportNamesMissingArtifactCache() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Files.delete(fixture.cacheRoot().resolve(FIXTURE_CACHE_ENTRY));

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "time-service", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "INTEROP_TIME_SERVICE_LIVE_APPROVED",
                    "true",
                    "MJO_JVM_SERVER_COMMAND",
                    "/bin/true")));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(timeServicePrerequisiteReport(fixture, "jvm"), StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"artifact-cache\""), report);
    assertTrue(report.contains("cache entry is missing"), report);
  }

  @Test
  void timeServicePrerequisiteReportNamesMissingDigestPinnedBaseImage() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "time-service", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "INTEROP_TIME_SERVICE_LIVE_APPROVED",
                    "true",
                    "MJO_JVM_SERVER_COMMAND",
                    "/bin/true")));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(timeServicePrerequisiteReport(fixture, "jvm"), StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"INTEROP_JAVA_BASE_IMAGE\""), report);
  }

  @Test
  void timeServicePrerequisiteReportNamesMissingContainerRuntime() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path isolatedBin = isolatedPathWithoutContainerRuntime();

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "time-service", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "PATH",
                    isolatedBin.toString(),
                    "INTEROP_TIME_SERVICE_LIVE_APPROVED",
                    "true",
                    "MJO_JVM_SERVER_COMMAND",
                    "/bin/true",
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(timeServicePrerequisiteReport(fixture, "jvm"), StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"container-runtime\""), report);
  }

  @Test
  void timeServicePrerequisiteReportNamesMissingPeerImage() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("time-service-missing-image", 1, 0);

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "time-service", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "INTEROP_TIME_SERVICE_LIVE_APPROVED",
                    "true",
                    "MJO_JVM_SERVER_COMMAND",
                    "/bin/true",
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(timeServicePrerequisiteReport(fixture, "jvm"), StandardCharsets.UTF_8);
    assertTrue(report.contains("\"missingPrerequisite\": \"peer-image\""), report);
  }

  @Test
  void aceTimeServiceUnsupportedReportsDoNotMaskMissingPrerequisites() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path acePeer = fixture.root().resolve("interop/peers/ace-tao");
    Files.createDirectories(acePeer);
    Files.writeString(
        acePeer.resolve("peer.yaml"),
        peerManifest(FixtureOptions.valid()).replace("fixture-peer", "ace-tao"),
        StandardCharsets.UTF_8);

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "time-service", "ace-tao"),
            fixture.environmentWithCache(
                Map.of(
                    "INTEROP_TIME_SERVICE_LIVE_APPROVED",
                    "true",
                    "INTEROP_NATIVE_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            timeServicePrerequisiteReport(fixture, "ace-tao", "jvm"), StandardCharsets.UTF_8);
    assertTrue(report.contains("\"classification\": \"missing-prerequisite\""), report);
    assertTrue(report.contains("\"missingPrerequisite\": \"MJO_JVM_SERVER_COMMAND\""), report);
    assertFalse(report.contains("\"classification\": \"unsupported-scenario\""), report);
  }

  @Test
  void timeServiceLiveDirectionMatrixRecordsCheckedReportsWhenPrerequisitesPass() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("time-service-live", 0, 0);
    Path jvmClient = successfulLaneCommand("time-service-jvm-client");
    Path nativeClient = successfulLaneCommand("time-service-native-client");
    Path jvmServer = serverLaneCommand("time-service-jvm-server");
    Path nativeServer = serverLaneCommand("time-service-native-server");

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "time-service", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.ofEntries(
                    Map.entry("CONTAINER_RUNTIME", runtime.toString()),
                    Map.entry("INTEROP_TIME_SERVICE_LIVE_APPROVED", "true"),
                    Map.entry("INTEROP_HEALTH_DELAY_SECONDS", "0"),
                    Map.entry("INTEROP_LOCAL_SERVER_START_DELAY_SECONDS", "1"),
                    Map.entry("INTEROP_LOCAL_SERVER_IOR_ATTEMPTS", "20"),
                    Map.entry("INTEROP_LOCAL_SERVER_IOR_DELAY_SECONDS", "0"),
                    Map.entry("INTEROP_JAVA_BASE_IMAGE", DIGEST_PINNED_BASE_IMAGE),
                    Map.entry("MJO_JVM_CLIENT_COMMAND", jvmClient.toString()),
                    Map.entry("MJO_NATIVE_CLIENT_BINARY", nativeClient.toString()),
                    Map.entry("MJO_JVM_SERVER_COMMAND", jvmServer.toString()),
                    Map.entry("MJO_NATIVE_SERVER_BINARY", nativeServer.toString()))));

    assertSuccess(result);
    String jvmClientReport =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/local/reports/"
                        + "time-service-fixture-peer-jvm-client-peer-server-to-local-client.json"),
            StandardCharsets.UTF_8);
    String peerClientReport =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/fixture/reports/"
                        + "time-service-jvm-server-to-peer-client.json"),
            StandardCharsets.UTF_8);
    String peerServerReport =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/time-service-server.json"),
            StandardCharsets.UTF_8);

    assertTrue(jvmClientReport.contains("\"classification\": \"time-service-checked\""));
    assertTrue(peerClientReport.contains("\"classification\": \"time-service-checked\""));
    assertTrue(
        peerClientReport.contains(
            "\"operationSet\": \"universal_time,new_universal_time,new_interval\""));
    assertTrue(peerServerReport.contains("\"classification\": \"server-ready\""));
  }

  @Test
  void durablePeerRawEvidencePathsRemainIgnored() throws Exception {
    String gitignore = Files.readString(repoRoot().resolve(".gitignore"));

    assertTrue(gitignore.contains("build/"), gitignore);
    assertTrue(gitignore.contains("interop/work/"), gitignore);
    assertTrue(gitignore.contains("interop/reports/"), gitignore);
  }

  @Test
  void directionMatrixReportsMissingLocalPrerequisites() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("matrix-live", 0, 0);

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "basic-idl", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "INTEROP_HEALTH_DELAY_SECONDS",
                    "0",
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE)));

    assertEquals(1, result.exitCode(), result.output());
    String clientReport =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/local/reports/"
                        + "basic-idl-fixture-peer-jvm-client-peer-server-to-local-client.json"),
            StandardCharsets.UTF_8);
    String serverReport =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/local/reports/"
                        + "basic-idl-fixture-peer-jvm-server-local-server-to-peer-client.json"),
            StandardCharsets.UTF_8);
    String nativeClientReport =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/local/reports/"
                        + "basic-idl-fixture-peer-native-client-peer-server-to-local-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(clientReport.contains("G10-120 jvm client command prerequisite missing"));
    assertTrue(serverReport.contains("G10-120 jvm server command prerequisite missing"));
    assertTrue(nativeClientReport.contains("G10-120 native client command prerequisite missing"));
  }

  @Test
  void rmiIiopLocalLaneClassifiesKnownWireFailuresAsProjectOwned() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path clientCommand = temporaryDirectory.resolve("rmi-client-fails.sh");
    Files.writeString(
        clientCommand,
        """
        #!/usr/bin/env bash
        printf 'io.github.mundanej.mjo.rmi.iiop.RmiIiopWireException: Malformed RMI-IIOP reply body\\n' >&2
        exit 1
        """,
        StandardCharsets.UTF_8);
    clientCommand.toFile().setExecutable(true);
    Path runtime = fakeContainerRuntime("rmi-wire-classification", 0, 0);

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "rmi-iiop", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE,
                    "INTEROP_HEALTH_DELAY_SECONDS",
                    "0",
                    "MJO_JVM_CLIENT_COMMAND",
                    clientCommand.toString())));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/local/reports/"
                        + "rmi-iiop-fixture-peer-jvm-client-peer-server-to-local-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"classification\": \"our-bug\""), report);
    assertTrue(report.contains("G10-120-080 project-owned defect closure"), report);
  }

  @Test
  void rmiIiopClassifierDoesNotTreatGenericBadOperationAsProjectOwned() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path clientCommand = temporaryDirectory.resolve("generic-bad-operation.sh");
    Files.writeString(
        clientCommand,
        """
        #!/usr/bin/env bash
        printf 'org.omg.CORBA.BAD_OPERATION: unknown operation\\n' >&2
        exit 1
        """,
        StandardCharsets.UTF_8);
    clientCommand.toFile().setExecutable(true);
    Path runtime = fakeContainerRuntime("rmi-generic-bad-operation", 0, 0);

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "rmi-iiop", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE,
                    "INTEROP_HEALTH_DELAY_SECONDS",
                    "0",
                    "MJO_JVM_CLIENT_COMMAND",
                    clientCommand.toString())));

    assertEquals(1, result.exitCode(), result.output());
    String report =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/local/reports/"
                        + "rmi-iiop-fixture-peer-jvm-client-peer-server-to-local-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(report.contains("\"classification\": \"infrastructure-failure\""), report);
  }

  @Test
  void directionMatrixRequiresLocalServerIorBeforePeerClientExecution() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("matrix-ior-readiness", 0, 0);
    Path staleIor = fixture.root().resolve("build/interop/fixture/iors/basic-idl-server.ior");
    Path staleIorParent = staleIor.getParent();
    assertNotNull(staleIorParent);
    Files.createDirectories(staleIorParent);
    Files.writeString(staleIor, "IOR:stale", StandardCharsets.UTF_8);
    Path serverCommand = temporaryDirectory.resolve("server-without-ior.sh");
    Files.writeString(
        serverCommand,
        """
        #!/usr/bin/env bash
        sleep 30
        """,
        StandardCharsets.UTF_8);
    serverCommand.toFile().setExecutable(true);

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "basic-idl", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "INTEROP_HEALTH_DELAY_SECONDS",
                    "0",
                    "INTEROP_LOCAL_SERVER_START_DELAY_SECONDS",
                    "0",
                    "INTEROP_LOCAL_SERVER_IOR_ATTEMPTS",
                    "1",
                    "INTEROP_LOCAL_SERVER_IOR_DELAY_SECONDS",
                    "0",
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE,
                    "MJO_JVM_SERVER_COMMAND",
                    serverCommand.toString())));

    assertEquals(1, result.exitCode(), result.output());
    String serverReport =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/local/reports/"
                        + "basic-idl-fixture-peer-jvm-server-local-server-to-peer-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(
        serverReport.contains("G10-120 jvm server IOR was not ready before peer client execution"),
        serverReport);
    assertFalse(Files.exists(staleIor), "stale local-server IOR must be removed before startup");
  }

  @Test
  void directionMatrixLabelsNativeServerRuntimeWhenPeerClientRuns() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("matrix-native-server", 0, 0);
    Path nativeServer = temporaryDirectory.resolve("native-server.sh");
    Files.writeString(
        nativeServer,
        """
        #!/usr/bin/env bash
        set -euo pipefail
        mkdir -p "$(dirname "${MJO_INTEROP_SERVER_IOR}")"
        printf 'IOR:native-server\\n' >"${MJO_INTEROP_SERVER_IOR}"
        sleep 30
        """,
        StandardCharsets.UTF_8);
    nativeServer.toFile().setExecutable(true);

    CommandResult result =
        run(
            command("run-direction-matrix", "--require-live", "basic-idl", FIXTURE_PEER),
            fixture.environmentWithCache(
                Map.of(
                    "CONTAINER_RUNTIME",
                    runtime.toString(),
                    "INTEROP_HEALTH_DELAY_SECONDS",
                    "0",
                    "INTEROP_LOCAL_SERVER_START_DELAY_SECONDS",
                    "1",
                    "INTEROP_LOCAL_SERVER_IOR_ATTEMPTS",
                    "1",
                    "INTEROP_LOCAL_SERVER_IOR_DELAY_SECONDS",
                    "0",
                    "INTEROP_JAVA_BASE_IMAGE",
                    DIGEST_PINNED_BASE_IMAGE,
                    "MJO_NATIVE_SERVER_BINARY",
                    nativeServer.toString())));

    assertEquals(1, result.exitCode(), result.output());
    String peerClientReport =
        Files.readString(
            fixture
                .root()
                .resolve(
                    "build/interop/fixture/reports/basic-idl-native-server-to-peer-client.json"),
            StandardCharsets.UTF_8);
    assertTrue(
        peerClientReport.contains("\"role\": \"native-server-to-peer-client\""), peerClientReport);
    assertTrue(peerClientReport.contains("\"serverRuntime\": \"our-native\""), peerClientReport);
  }

  @Test
  void undeclaredScenarioIsRejectedBeforeDryRunOrLiveExecution() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(
            command("run-scenario", "--dry-run", "not-declared", FIXTURE_PEER),
            fixture.environment());

    assertFailure(result, "scenario is not declared: not-declared");
    assertFalse(Files.exists(fixture.root().resolve("build/interop/fixture/reports")));
  }

  @Test
  void interopGateToolDoesNotDownloadOrVendorPeerArtifacts() throws Exception {
    String script = Files.readString(repoRoot().resolve("interop/bin/interop-peer"));

    for (String forbidden : List.of("curl ", "wget ", "git clone", "mvn dependency:get")) {
      assertFalse(script.contains(forbidden), "script must not contain " + forbidden);
    }
    assertTrue(script.contains("INTEROP_ARTIFACT_CACHE"));
    assertTrue(script.contains("vendoredPeerArtifacts must be false"));
  }

  @Test
  void reportHarnessKeepsCleanRoomBoundary() throws Exception {
    List<Path> files = new ArrayList<>();
    files.add(repoRoot().resolve("interop/bin/interop-peer"));
    try (var stream =
        Files.walk(repoRoot().resolve("modules/corba-interop-testkit/src/main/java"))) {
      stream.filter(Files::isRegularFile).forEach(files::add);
    }

    List<String> forbiddenTokens =
        List.of(
            "java.lang.reflect",
            "Proxy.newProxyInstance",
            "ClassLoader",
            "ServiceLoader",
            "ObjectInputStream",
            "ObjectOutputStream",
            "java.io.Serializable",
            "net.bytebuddy",
            "cglib",
            "classpath scan");
    for (Path file : files) {
      String content = Files.readString(file);
      for (String token : forbiddenTokens) {
        assertFalse(content.contains(token), file + " must not contain " + token);
      }
    }
  }

  private Fixture createFixture(FixtureOptions options)
      throws IOException, NoSuchAlgorithmException {
    Path root = temporaryDirectory.resolve("fixture-root-" + System.nanoTime());
    Path peers = root.resolve("interop/peers/" + FIXTURE_PEER);
    Path approvals = root.resolve("interop/approvals");
    Path cacheRoot = temporaryDirectory.resolve("cache-" + System.nanoTime()).toAbsolutePath();
    Path cacheEntry = cacheRoot.resolve(FIXTURE_CACHE_ENTRY);
    Files.createDirectories(peers);
    Files.createDirectories(approvals);
    Files.createDirectories(Objects.requireNonNull(cacheEntry.getParent()));
    Files.writeString(cacheEntry, FIXTURE_CONTENT, StandardCharsets.UTF_8);
    Path timeServiceIdl = root.resolve("interop/idl/time-service.idl");
    Files.createDirectories(Objects.requireNonNull(timeServiceIdl.getParent()));
    Files.copy(repoRoot().resolve("interop/idl/time-service.idl"), timeServiceIdl);
    Files.copy(
        repoRoot().resolve("interop/idl/event-service.idl"),
        root.resolve("interop/idl/event-service.idl"));
    Files.copy(
        repoRoot().resolve("interop/idl/notification-service.idl"),
        root.resolve("interop/idl/notification-service.idl"));

    Files.writeString(peers.resolve("peer.yaml"), peerManifest(options), StandardCharsets.UTF_8);
    Files.writeString(
        approvals.resolve("fixture-peer.approval.yaml"), approval(options), StandardCharsets.UTF_8);
    return new Fixture(root.toAbsolutePath(), cacheRoot);
  }

  private static String peerManifest(FixtureOptions options) {
    return """
                name: fixture-peer
                displayName: Fixture Peer
                status: g6-artifact-gate-approved
                gate: G6-820
                asOfDate: "2026-05-18"
                peerType: jvm
                requirementIds:
                  - REQ-INTEROP-005
                roles:
                  - client
                  - server
                scenarioGroups:
                  - basic-idl
                  - rmi-iiop
                  - time-service
                  - event-service
                  - notification-service
                  - g12-wide-core-types
                  - g13-durable-ior-peer-client-restart
                  - g13-durable-naming-peer-client-restart
                scenarioCapabilities:
                  basic-idl:
                    idl: interop/idl/basic/BasicTypes.idl
                    support: live-object-reference-smoke
                    directions:
                      - peer-server-to-local-client
                      - local-server-to-peer-client
                    localRuntimes:
                      - jvm
                      - native
                    expectedClassifications:
                      - expected-deferral
                  rmi-iiop:
                    idl: interop/idl/rmi-iiop/Calculator.idl
                    support: live-calculator-smoke
                    directions:
                      - peer-server-to-local-client
                      - local-server-to-peer-client
                    localRuntimes:
                      - jvm
                      - native
                    expectedClassifications:
                      - calculator-checked
                      - expected-deferral
                  time-service:
                    idl: interop/idl/time-service.idl
                    support: live-time-service-smoke
                    directions:
                      - peer-server-to-local-client
                      - local-server-to-peer-client
                    localRuntimes:
                      - jvm
                      - native
                    expectedClassifications:
                      - time-service-checked
                      - server-ready
                      - expected-deferral
                      - missing-prerequisite
                      - unsupported-scenario
                      - infrastructure-failure
                  event-service:
                    idl: interop/idl/event-service.idl
                    support: event-service-metadata-dry-run
                    directions:
                      - peer-server-to-local-client
                      - local-server-to-peer-client
                    localRuntimes:
                      - jvm
                      - native
                    expectedClassifications:
                      - expected-deferral
                      - missing-prerequisite
                      - unsupported-scenario
                      - infrastructure-failure
                  notification-service:
                    idl: interop/idl/notification-service.idl
                    support: notification-service-metadata-dry-run
                    directions:
                      - peer-server-to-local-client
                      - local-server-to-peer-client
                    localRuntimes:
                      - jvm
                      - native
                    expectedClassifications:
                      - expected-deferral
                      - missing-prerequisite
                      - unsupported-scenario
                      - infrastructure-failure
                  g12-wide-core-types:
                    idl: interop/idl/g12-wide/CoreTypes.idl
                    support: live-mounted-idl-object-reference-smoke
                    directions:
                      - peer-server-to-local-client
                      - local-server-to-peer-client
                    localRuntimes:
                      - jvm
                      - native
                    expectedClassifications:
                      - expected-deferral
                  g13-durable-ior-peer-client-restart:
                    idl: interop/idl/object-reference.idl
                    support: durable-ior-peer-client-restart
                    directions:
                      - local-server-to-peer-client
                    localRuntimes:
                      - jvm
                      - native
                    expectedClassifications:
                      - durable-ior-invoked
                      - server-ready
                      - expected-deferral
                      - missing-prerequisite
                  g13-durable-naming-peer-client-restart:
                    idl: interop/idl/naming.idl
                    support: durable-naming-peer-client-restart
                    directions:
                      - local-server-to-peer-client
                    localRuntimes:
                      - jvm
                      - native
                    expectedClassifications:
                      - durable-naming-resolved
                      - server-ready
                      - expected-deferral
                      - missing-prerequisite
                candidateOrigin:
                  kind: maven
                  coordinate: "example:fixture:1.0"
                  version: "1.0"
                  repository: Fixture
                  project: Fixture
                  sourceUrl: "https://example.invalid/fixture-1.0.jar"
                license:
                  observed: fixture-license
                  reviewRequired: true
                  approvalStatus: approved-for-black-box-interop
                  notes: "Fixture approval."
                approval: interop/approvals/fixture-peer.approval.yaml
                cleanRoom:
                  sourceCopyingAllowed: false
                  sourceVendoringAllowed: false
                  binaryVendoringAllowed: false
                  allowedUses:
                    - black-box-interop
                  prohibitedUses:
                    - source-copying
                container:
                  imageName: corba-interop-peer-fixture:1.0
                  buildStatus: gate-approved
                  buildFile: interop/container/Containerfile.jvm.template
                  buildScript: interop/peers/fixture-peer/build-image.sh
                  baseImagePolicy: digest-pinned-required
                  artifactInputs:
                    - "example:fixture:1.0"
                  artifactCache: external-only
                  vendoredPeerArtifacts: __VENDORED_PEER_ARTIFACTS__
                  ports:
                    iiop: 2809
                launch:
                  clientCommand: "interop/peers/fixture-peer/launch.sh client"
                  serverCommand: "interop/peers/fixture-peer/launch.sh server"
                  namingCommand: "interop/peers/fixture-peer/launch.sh naming"
                  readinessCheck: interop/peers/fixture-peer/health.sh
                containerCommands:
                  client: client
                  server: server
                  naming: naming
                  health: health
                  report: report
                reports:
                  logs: "build/interop/fixture/logs"
                  iors: "build/interop/fixture/iors"
                  structuredReports: "build/interop/fixture/reports"
                """
        .replace("__VENDORED_PEER_ARTIFACTS__", Boolean.toString(options.vendoredPeerArtifacts()));
  }

  private static String removeG12CoreCapability(String manifest) {
    return replaceG12CoreCapability(manifest, "");
  }

  private static String replaceG12CoreCapability(String manifest, String replacement) {
    int start = manifest.indexOf("  g12-wide-core-types:\n");
    int end = manifest.indexOf("candidateOrigin:", start);
    if (start < 0 || end < 0) {
      throw new IllegalArgumentException("G12 core capability block not found");
    }
    return manifest.substring(0, start) + replacement + manifest.substring(end);
  }

  private static String removeEventServiceCapability(String manifest) {
    String withoutGroup = manifest.replace("  - event-service\n", "");
    int start = withoutGroup.indexOf("  event-service:\n");
    int end = withoutGroup.indexOf("  notification-service:\n", start);
    if (start < 0 || end < 0) {
      throw new IllegalArgumentException("Event Service capability block not found");
    }
    return withoutGroup.substring(0, start) + withoutGroup.substring(end);
  }

  private static String removeNotificationServiceCapability(String manifest) {
    String withoutGroup = manifest.replace("  - notification-service\n", "");
    int start = withoutGroup.indexOf("  notification-service:\n");
    int end = withoutGroup.indexOf("  g12-wide-core-types:\n", start);
    if (start < 0 || end < 0) {
      throw new IllegalArgumentException("Notification Service capability block not found");
    }
    return withoutGroup.substring(0, start) + withoutGroup.substring(end);
  }

  private static Map<String, String> eventServiceApprovedCommandEnvironment() {
    java.util.HashMap<String, String> environment = new java.util.HashMap<>();
    environment.put("INTEROP_EVENT_SERVICE_LIVE_APPROVED", "true");
    environment.put("MJO_JVM_CLIENT_COMMAND", "/bin/true");
    environment.put("MJO_JVM_SERVER_COMMAND", "/bin/true");
    environment.put("MJO_NATIVE_CLIENT_BINARY", "/bin/true");
    environment.put("MJO_NATIVE_SERVER_BINARY", "/bin/true");
    return environment;
  }

  private static Map<String, String> notificationServiceApprovedCommandEnvironment() {
    java.util.HashMap<String, String> environment = new java.util.HashMap<>();
    environment.put("INTEROP_NOTIFICATION_SERVICE_LIVE_APPROVED", "true");
    environment.put("MJO_JVM_CLIENT_COMMAND", "/bin/true");
    environment.put("MJO_JVM_SERVER_COMMAND", "/bin/true");
    environment.put("MJO_NATIVE_CLIENT_BINARY", "/bin/true");
    environment.put("MJO_NATIVE_SERVER_BINARY", "/bin/true");
    return environment;
  }

  private static Path eventServicePrerequisiteReport(
      Fixture fixture, String peer, String runtime, String role, String direction) {
    return fixture
        .root()
        .resolve(
            "build/interop/local/reports/event-service-"
                + peer
                + "-"
                + runtime
                + "-"
                + role
                + "-"
                + direction
                + ".json");
  }

  private static Path notificationServicePrerequisiteReport(
      Fixture fixture, String peer, String runtime, String role, String direction) {
    return fixture
        .root()
        .resolve(
            "build/interop/local/reports/notification-service-"
                + peer
                + "-"
                + runtime
                + "-"
                + role
                + "-"
                + direction
                + ".json");
  }

  private static Path timeServicePrerequisiteReport(Fixture fixture, String runtime) {
    return timeServicePrerequisiteReport(fixture, FIXTURE_PEER, runtime);
  }

  private static Path timeServicePrerequisiteReport(Fixture fixture, String peer, String runtime) {
    return fixture
        .root()
        .resolve(
            "build/interop/local/reports/time-service-"
                + peer
                + "-"
                + runtime
                + "-server-local-server-to-peer-client.json");
  }

  private static String approval(FixtureOptions options) throws NoSuchAlgorithmException {
    return """
                peer: __APPROVAL_PEER__
                taskId: G6-820-REAL-PEER-ARTIFACT-GATES
                approvalStatus: __APPROVAL_STATUS__
                reviewer: fixture-reviewer
                reviewDate: "2026-05-18"
                approvalEvidence:
                  decisionRecord: fixture-decision
                  scope: fixture black-box gates
                artifact:
                  originKind: maven
                  coordinate: "example:fixture:1.0"
                  version: "1.0"
                  sourceUrl: "https://example.invalid/fixture-1.0.jar"
                license:
                  observed: fixture-license
                  reviewRequired: true
                  approvalStatus: __LICENSE_APPROVAL_STATUS__
                  allowedUse: black-box-interop-only
                  redistributionAllowed: false
                  containerPublicationAllowed: false
                cleanRoom:
                  sourceCopyingAllowed: false
                  sourceVendoringAllowed: false
                  binaryVendoringAllowed: false
                  allowedUses:
                    - black-box-interop
                  prohibitedUses:
                    - source-copying
                cache:
                  layout: external-only
                  baseImagePolicy: digest-pinned-required
                  entries:
                    - path: __CACHE_ENTRY__
                      sha256: __CACHE_SHA256__
                      sourceUrl: "https://example.invalid/fixture-1.0.jar"
                      packaging: jar
                realPeerExecution:
                  status: blocked-until-g6-830
                """
        .replace("__APPROVAL_PEER__", options.approvalPeer())
        .replace("__APPROVAL_STATUS__", options.approvalStatus())
        .replace("__LICENSE_APPROVAL_STATUS__", options.approvalStatus())
        .replace("__CACHE_ENTRY__", FIXTURE_CACHE_ENTRY)
        .replace("__CACHE_SHA256__", sha256(FIXTURE_CONTENT));
  }

  private CommandResult run(List<String> command, Map<String, String> environment)
      throws Exception {
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.directory(repoRoot().toFile());
    builder.environment().putAll(environment);
    Process process = builder.start();
    String output =
        new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
            + new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    return new CommandResult(process.waitFor(), output);
  }

  private static List<String> command(String... arguments) {
    List<String> command = new ArrayList<>();
    command.add(repoRoot().resolve("interop/bin/interop-peer").toString());
    command.addAll(Arrays.asList(arguments));
    return command;
  }

  private static Path repoRoot() {
    Path moduleRoot = Path.of("").toAbsolutePath();
    Path modulesRoot = Objects.requireNonNull(moduleRoot.getParent());
    return Objects.requireNonNull(modulesRoot.getParent());
  }

  private static Path executableOnPath(String name) {
    String path = System.getenv("PATH");
    if (path != null) {
      for (String entry : pathEntries(path)) {
        Path candidate = Path.of(entry).resolve(name);
        if (Files.isExecutable(candidate)) {
          return candidate;
        }
      }
    }
    throw new IllegalStateException("Required test executable not found on PATH: " + name);
  }

  private Path fakeContainerRuntime(String name, int inspectExitCode, int runExitCode)
      throws IOException {
    return fakeContainerRuntime(name, inspectExitCode, runExitCode, 0);
  }

  private Path fakeContainerRuntime(
      String name, int inspectExitCode, int runExitCode, int networkExitCode) throws IOException {
    Path runtime = temporaryDirectory.resolve(name);
    Files.writeString(
        runtime,
        """
        #!/usr/bin/env bash
        set -euo pipefail
        if [[ -n "${FAKE_RUNTIME_LOG:-}" ]]; then
          printf '%s\\n' "$*" >>"${FAKE_RUNTIME_LOG}"
        fi
        if [[ "${1:-}" == "image" && "${2:-}" == "inspect" ]]; then
          exit __INSPECT_EXIT__
        fi
        if [[ "${1:-}" == "network" ]]; then
          exit __NETWORK_EXIT__
        fi
        if [[ "${1:-}" == "run" ]]; then
          scenario="basic-idl"
          iors=""
          for arg in "$@"; do
            case "${arg}" in
              INTEROP_SCENARIO=*) scenario="${arg#INTEROP_SCENARIO=}" ;;
              *:/interop/iors) iors="${arg%:/interop/iors}" ;;
            esac
          done
          if [[ "$*" == *"INTEROP_ROLE=server"* && -n "${iors}" ]]; then
            mkdir -p "${iors}"
            printf 'IOR:fresh-peer\\n' >"${iors}/${scenario}-server.ior"
          fi
          exit __RUN_EXIT__
        fi
        if [[ "${1:-}" == "rm" ]]; then
          exit 0
        fi
        exit 64
        """
            .replace("__INSPECT_EXIT__", Integer.toString(inspectExitCode))
            .replace("__RUN_EXIT__", Integer.toString(runExitCode))
            .replace("__NETWORK_EXIT__", Integer.toString(networkExitCode)),
        StandardCharsets.UTF_8);
    runtime.toFile().setExecutable(true);
    return runtime;
  }

  private Path successfulLaneCommand(String name) throws IOException {
    Path command = temporaryDirectory.resolve(name + ".sh");
    Files.writeString(
        command,
        """
        #!/usr/bin/env bash
        set -euo pipefail
        printf 'lane ok\\n'
        """,
        StandardCharsets.UTF_8);
    command.toFile().setExecutable(true);
    return command;
  }

  private Path serverLaneCommand(String name) throws IOException {
    Path command = temporaryDirectory.resolve(name + ".sh");
    Files.writeString(
        command,
        """
        #!/usr/bin/env bash
        set -euo pipefail
        mkdir -p "$(dirname "${MJO_INTEROP_SERVER_IOR}")"
        printf 'IOR:%s\\n' "${MJO_INTEROP_SCENARIO}" >"${MJO_INTEROP_SERVER_IOR}"
        sleep 30
        """,
        StandardCharsets.UTF_8);
    command.toFile().setExecutable(true);
    return command;
  }

  private Path isolatedPathWithoutContainerRuntime() throws IOException {
    Path bin = temporaryDirectory.resolve("isolated-bin-" + System.nanoTime());
    Files.createDirectories(bin);
    for (String executable : List.of("bash", "python3", "date", "basename", "dirname", "mkdir")) {
      Files.createSymbolicLink(bin.resolve(executable), executableOnPath(executable));
    }
    return bin;
  }

  private static List<String> pathEntries(String path) {
    List<String> entries = new ArrayList<>();
    int start = 0;
    while (start <= path.length()) {
      int separator = path.indexOf(java.io.File.pathSeparatorChar, start);
      int end = separator == -1 ? path.length() : separator;
      if (end > start) {
        entries.add(path.substring(start, end));
      }
      if (separator == -1) {
        return entries;
      }
      start = separator + 1;
    }
    return entries;
  }

  private static void assertSuccess(CommandResult result) {
    assertEquals(0, result.exitCode(), result.output());
  }

  private static void assertFailure(CommandResult result, String expectedMessage) {
    assertTrue(result.exitCode() != 0, result.output());
    assertTrue(result.output().contains(expectedMessage), result.output());
  }

  private static String sha256(String value) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
    StringBuilder builder = new StringBuilder(hashed.length * 2);
    for (byte item : hashed) {
      int valueByte = item & 0xff;
      builder.append(Character.forDigit((valueByte >>> 4) & 0xf, 16));
      builder.append(Character.forDigit(valueByte & 0xf, 16));
    }
    return builder.toString();
  }

  private record CommandResult(int exitCode, String output) {}

  private record Fixture(Path root, Path cacheRoot) {
    Map<String, String> environment() {
      return Map.of("INTEROP_ROOT", root.toString());
    }

    Map<String, String> environmentWithCache() {
      return environmentWithCache(Map.of());
    }

    Map<String, String> environmentWithCache(Map<String, String> extra) {
      Map<String, String> environment = new java.util.HashMap<>(extra);
      environment.put("INTEROP_ROOT", root.toString());
      environment.put("INTEROP_ARTIFACT_CACHE", cacheRoot.toString());
      return environment;
    }
  }

  private record FixtureOptions(
      String approvalStatus, String approvalPeer, boolean vendoredPeerArtifacts) {
    static FixtureOptions valid() {
      return new FixtureOptions("approved-for-black-box-interop", FIXTURE_PEER, false);
    }

    FixtureOptions withApprovalStatus(String status) {
      return new FixtureOptions(status, approvalPeer, vendoredPeerArtifacts);
    }

    FixtureOptions withApprovalPeer(String peer) {
      return new FixtureOptions(approvalStatus, peer, vendoredPeerArtifacts);
    }

    FixtureOptions withVendoredPeerArtifacts(boolean vendored) {
      return new FixtureOptions(approvalStatus, approvalPeer, vendored);
    }
  }
}
