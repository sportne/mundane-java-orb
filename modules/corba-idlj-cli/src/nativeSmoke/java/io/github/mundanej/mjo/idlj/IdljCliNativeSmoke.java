package io.github.mundanej.mjo.idlj;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Native Image smoke entry point for the idlj validate command. */
public final class IdljCliNativeSmoke {

  private IdljCliNativeSmoke() {}

  /** Runs compact CLI validation assertions under Native Image. */
  public static void main(String[] args) throws Exception {
    Path directory = Files.createTempDirectory("idlj-native-smoke");
    Path valid = directory.resolve("valid.idl");
    Path invalid = directory.resolve("invalid.idl");
    Path lineMarker = directory.resolve("line-marker.idl");
    Files.writeString(
        valid,
        "module NativeSmoke { interface Service { void ping(in long value); }; };\n",
        StandardCharsets.UTF_8);
    Files.writeString(
        invalid,
        "module NativeSmoke { struct Point { long x; short X; }; };\n",
        StandardCharsets.UTF_8);
    Files.writeString(
        lineMarker,
        """
        #line 12 "native-original.idl"
        module NativeSmoke { struct Point { long x; short X; }; };
        """,
        StandardCharsets.UTF_8);

    Run validRun = run("validate", "--quiet", valid.toString());
    requireEquals(IdljExitCodes.SUCCESS, validRun.exitCode(), "valid exit code");
    requireEquals("", validRun.stdout(), "valid stdout");
    requireEquals("", validRun.stderr(), "valid stderr");

    Run invalidRun = run("validate", invalid.toString());
    requireEquals(IdljExitCodes.VALIDATION_FAILED, invalidRun.exitCode(), "invalid exit code");
    require(invalidRun.stdout().isEmpty(), "invalid stdout");
    require(invalidRun.stderr().contains("ERROR IDL-0400"), "invalid diagnostic");

    Run lineMarkerRun = run("validate", lineMarker.toString());
    requireEquals(
        IdljExitCodes.VALIDATION_FAILED, lineMarkerRun.exitCode(), "line marker exit code");
    require(
        lineMarkerRun.stderr().contains("native-original.idl:12:45: ERROR IDL-0400"),
        "line marker diagnostic: " + lineMarkerRun.stderr());
  }

  private static Run run(String... args) {
    StringWriter stdout = new StringWriter();
    StringWriter stderr = new StringWriter();
    int exitCode =
        new IdljCli().run(args, new PrintWriter(stdout, true), new PrintWriter(stderr, true));
    return new Run(exitCode, stdout.toString(), stderr.toString());
  }

  private static void require(boolean condition, String label) {
    if (!condition) {
      throw new AssertionError("idlj native smoke failed: " + label);
    }
  }

  private static void requireEquals(int expected, int actual, String label) {
    if (expected != actual) {
      throw new AssertionError(
          "idlj native smoke failed: " + label + "; expected " + expected + ", got " + actual);
    }
  }

  private static void requireEquals(String expected, String actual, String label) {
    if (!expected.equals(actual)) {
      throw new AssertionError(
          "idlj native smoke failed: " + label + "; expected " + expected + ", got " + actual);
    }
  }

  private record Run(int exitCode, String stdout, String stderr) {}
}
