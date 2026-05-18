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
  void approvedFixtureValidatesWithRequiredCache() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(command("validate-gates", "--require-cache"), fixture.environmentWithCache());

    assertSuccess(result);
    assertTrue(
        result.output().contains(FIXTURE_PEER + ": interop/approvals/fixture-peer.approval.yaml"));
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

    CommandResult result = run(command("report", FIXTURE_PEER), environment);

    assertSuccess(result);
    String summary =
        Files.readString(
            fixture.root().resolve("build/interop/fixture/reports/summary.json"),
            StandardCharsets.UTF_8);
    assertTrue(summary.contains("\"peer\": \"fixture-peer\""), summary);
    assertTrue(summary.contains("\"reportCount\": 2"), summary);
    assertTrue(summary.contains("basic-idl-server.json"), summary);
    assertTrue(summary.contains("report-report.json"), summary);
  }

  @Test
  void runScenarioDryRunRemainsNonMutating() throws Exception {
    Fixture fixture = createFixture(FixtureOptions.valid());

    CommandResult result =
        run(command("run-scenario", "--dry-run", "basic-idl", FIXTURE_PEER), fixture.environment());

    assertSuccess(result);
    assertTrue(
        result
            .output()
            .contains("dry-run: would run scenario basic-idl role server for fixture-peer"));
    assertTrue(
        result
            .output()
            .contains("dry-run: would run scenario basic-idl role client for fixture-peer"));
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
