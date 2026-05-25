package io.github.mundanej.mjo.interop.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
  void actualPeerManifestsDeclareRmiIiopScenario() throws Exception {
    for (String peer : REAL_PEERS) {
      String manifest =
          Files.readString(repoRoot().resolve("interop/peers/" + peer + "/peer.yaml"));

      assertTrue(manifest.contains("scenarioGroups:"), manifest);
      assertTrue(manifest.contains("  - rmi-iiop"), manifest);
    }
    assertTrue(
        Files.exists(repoRoot().resolve("interop/idl/rmi-iiop/Calculator.idl")),
        "RMI-IIOP IDL fixture must be present");
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
    assertTrue(containerfile.contains("ace_tao_peer.cpp"), containerfile);
    assertTrue(peerEntry.contains("unsupported-scenario"), peerEntry);
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
    assertTrue(
        peerEntry.contains("Standalone ACE/TAO health requires a running scenario server"),
        peerEntry);
    assertFalse(cpp.contains("ACE_TAO_PLACEHOLDER"), cpp);
    assertFalse(cpp.contains("system("), cpp);
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
    assertTrue(runtimeCalls.contains("INTEROP_ROLE=health"));
    assertTrue(runtimeCalls.contains("INTEROP_ROLE=client"));
    assertTrue(runtimeCalls.contains("rm -f mjo-fixture-peer-basic-idl-server-"));
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
  void directionMatrixRequiresLocalServerIorBeforePeerClientExecution() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());
    Path runtime = fakeContainerRuntime("matrix-ior-readiness", 0, 0);
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
