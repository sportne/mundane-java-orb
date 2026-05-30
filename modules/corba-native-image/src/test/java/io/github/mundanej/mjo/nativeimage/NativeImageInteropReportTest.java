package io.github.mundanej.mjo.nativeimage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NativeImageInteropReportTest {

  @TempDir private Path temporaryDirectory;

  @Test
  void missingNativeClientBinaryProducesStructuredInfrastructureReport() throws Exception {
    CommandResult result =
        runInteropPeer("native-lane-report", "basic-idl", "client", Map.of("INTEROP_ROOT", root()));

    Path report =
        temporaryDirectory.resolve("build/interop/native/reports/basic-idl-native-client.json");
    assertEquals(1, result.exitCode(), result.stderr());
    assertTrue(Files.isRegularFile(report));
    String json = Files.readString(report);
    assertTrue(json.contains("\"clientRuntime\": \"our-native\""));
    assertTrue(json.contains("\"serverRuntime\": \"our-jvm-jdk21\""));
    assertTrue(json.contains("\"classification\": \"infrastructure-failure\""));
    assertTrue(json.contains("G10-100 native client binary prerequisite missing"));
  }

  @Test
  void missingG12WideNativeClientBinaryProducesStructuredInfrastructureReport() throws Exception {
    CommandResult result =
        runInteropPeer(
            "native-lane-report", "g12-wide-valuetypes", "client", Map.of("INTEROP_ROOT", root()));

    Path report =
        temporaryDirectory.resolve(
            "build/interop/native/reports/g12-wide-valuetypes-native-client.json");
    assertEquals(1, result.exitCode(), result.stderr());
    assertTrue(Files.isRegularFile(report));
    String json = Files.readString(report);
    assertTrue(json.contains("\"scenario\": \"g12-wide-valuetypes\""));
    assertTrue(json.contains("\"idl\": \"interop/idl/g12-wide/ValueTypes.idl\""));
    assertTrue(json.contains("\"classification\": \"infrastructure-failure\""));
    assertTrue(json.contains("G10-100 native client binary prerequisite missing"));
  }

  @Test
  void satisfiedNativeServerBinaryProducesStructuredPrerequisiteReport() throws Exception {
    Path binary = temporaryDirectory.resolve("server-smoke");
    Files.writeString(binary, "#!/usr/bin/env sh\nexit 0\n");
    binary.toFile().setExecutable(true);

    CommandResult result =
        runInteropPeer(
            "native-lane-report",
            "rmi-iiop",
            "server",
            Map.of("INTEROP_ROOT", root(), "MJO_NATIVE_SERVER_BINARY", binary.toString()));

    Path report =
        temporaryDirectory.resolve("build/interop/native/reports/rmi-iiop-native-server.json");
    assertEquals(0, result.exitCode(), result.stderr());
    assertTrue(Files.isRegularFile(report));
    String json = Files.readString(report);
    assertTrue(json.contains("\"clientRuntime\": \"our-jvm-jdk21\""));
    assertTrue(json.contains("\"serverRuntime\": \"our-native\""));
    assertTrue(json.contains("\"classification\": \"expected-deferral\""));
    assertTrue(json.contains("G10-100 native server binary prerequisite satisfied"));
  }

  @Test
  void failingNativeClientBinaryProducesStructuredFailureReport() throws Exception {
    Path binary = temporaryDirectory.resolve("client-smoke");
    Files.writeString(binary, "#!/usr/bin/env sh\nprintf 'native client failed' >&2\nexit 7\n");
    binary.toFile().setExecutable(true);

    CommandResult result =
        runInteropPeer(
            "native-lane-report",
            "basic-idl",
            "client",
            Map.of("INTEROP_ROOT", root(), "MJO_NATIVE_CLIENT_BINARY", binary.toString()));

    Path report =
        temporaryDirectory.resolve("build/interop/native/reports/basic-idl-native-client.json");
    Path stderr =
        temporaryDirectory.resolve("build/interop/native/logs/basic-idl-native-client.stderr.log");
    assertEquals(7, result.exitCode(), result.stderr());
    assertTrue(Files.isRegularFile(report));
    String json = Files.readString(report);
    assertTrue(json.contains("\"status\": \"failed\""));
    assertTrue(json.contains("\"classification\": \"infrastructure-failure\""));
    assertTrue(json.contains("\"exitCode\": 7"));
    assertTrue(json.contains("G10-100 native client binary exited with code 7"));
    assertEquals("native client failed", Files.readString(stderr).trim());
  }

  private String root() {
    return temporaryDirectory.toString();
  }

  private static CommandResult runInteropPeer(
      String command, String scenario, String role, Map<String, String> environment)
      throws IOException, InterruptedException {
    ProcessBuilder builder =
        new ProcessBuilder(
            Path.of("../..")
                .toAbsolutePath()
                .normalize()
                .resolve("interop/bin/interop-peer")
                .toString(),
            command,
            scenario,
            role);
    builder.environment().putAll(environment);
    Process process = builder.start();
    String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    return new CommandResult(process.waitFor(), stdout, stderr);
  }

  private record CommandResult(int exitCode, String stdout, String stderr) {}
}
