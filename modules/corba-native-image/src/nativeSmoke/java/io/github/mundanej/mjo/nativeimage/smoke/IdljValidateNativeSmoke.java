package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.idlj.IdljCli;
import io.github.mundanej.mjo.idlj.IdljExitCodes;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Native Image smoke entry point for the idlj validate binary. */
public final class IdljValidateNativeSmoke {

  private IdljValidateNativeSmoke() {}

  /** Runs compact validation checks. */
  public static void main(String[] args) throws Exception {
    Path directory = Files.createTempDirectory("g6-910-idlj");
    Path valid = directory.resolve("valid.idl");
    Path g12Wide = directory.resolve("g12-wide.idl");
    Path invalid = directory.resolve("invalid.idl");
    Files.writeString(
        valid,
        "module NativeImage { interface Service { string greet(in string name); }; };\n",
        StandardCharsets.UTF_8);
    Files.writeString(
        g12Wide,
        """
        #pragma prefix "example.org"
        module NativeWide {
          native Handle;
          abstract interface Marker {};
          valuetype Label string<32>;
          abstract valuetype BaseValue {};
          valuetype RichValue : BaseValue supports Marker {
            public Label label;
            public Handle handle;
            factory create(in Label label);
          };
        };
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        invalid,
        "module NativeImage { struct Point { long x; short X; }; };\n",
        StandardCharsets.UTF_8);

    Run validRun = run("validate", "--quiet", valid.toString());
    SmokeAssertions.requireEquals(IdljExitCodes.SUCCESS, validRun.exitCode(), "valid exit");
    SmokeAssertions.requireEquals("", validRun.stdout(), "valid stdout");
    SmokeAssertions.requireEquals("", validRun.stderr(), "valid stderr");

    Run g12WideRun = run("validate", "--quiet", g12Wide.toString());
    SmokeAssertions.requireEquals(IdljExitCodes.SUCCESS, g12WideRun.exitCode(), "G12 wide exit");
    SmokeAssertions.requireEquals("", g12WideRun.stdout(), "G12 wide stdout");
    SmokeAssertions.requireEquals("", g12WideRun.stderr(), "G12 wide stderr");

    Run invalidRun = run("validate", invalid.toString());
    SmokeAssertions.requireEquals(
        IdljExitCodes.VALIDATION_FAILED, invalidRun.exitCode(), "invalid exit");
    SmokeAssertions.require(invalidRun.stdout().isEmpty(), "invalid stdout");
    SmokeAssertions.require(invalidRun.stderr().contains("ERROR IDL-0400"), "invalid diagnostic");
  }

  private static Run run(String... args) {
    StringWriter stdout = new StringWriter();
    StringWriter stderr = new StringWriter();
    int exitCode =
        new IdljCli().run(args, new PrintWriter(stdout, true), new PrintWriter(stderr, true));
    return new Run(exitCode, stdout.toString(), stderr.toString());
  }

  private record Run(int exitCode, String stdout, String stderr) {}
}
