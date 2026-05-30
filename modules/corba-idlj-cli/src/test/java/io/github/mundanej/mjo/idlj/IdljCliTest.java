package io.github.mundanej.mjo.idlj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import io.github.mundanej.mjo.common.SourcePosition;
import io.github.mundanej.mjo.common.SourceSpan;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link IdljCli}. */
@Tag("unit")
final class IdljCliTest {

  @TempDir private Path tempDir;

  @Test
  void validatesOneOrMoreIdlFilesWithDeterministicSuccessOutput() throws Exception {
    Path first = write("first.idl", "module First { const long VALUE = 1; };\n");
    Path second = write("second.idl", "module Second { interface Service { void ping(); }; };\n");

    CliRun result = run("validate", first.toString(), second.toString());

    assertEquals(IdljExitCodes.SUCCESS, result.exitCode());
    assertEquals("Validated 2 IDL file(s).\n", result.stdout());
    assertEquals("", result.stderr());
  }

  @Test
  void supportsIncludePathFormsAndQuietSuccessOutput() throws Exception {
    Path includeDir = Files.createDirectory(tempDir.resolve("idl-includes"));
    Files.writeString(
        includeDir.resolve("shared.idl"),
        "struct Shared { long value; };\n",
        StandardCharsets.UTF_8);
    Path root =
        write(
            "root.idl",
            """
            #include "shared.idl"
            module Demo {
              struct Holder { Shared shared; };
            };
            """);

    assertQuietSuccess(run("validate", "--quiet", "-I" + includeDir, root.toString()));
    assertQuietSuccess(run("validate", "--quiet", "-I", includeDir.toString(), root.toString()));
    assertQuietSuccess(
        run("validate", "--quiet", "--include", includeDir.toString(), root.toString()));
  }

  @Test
  void returnsValidationFailureForParserDiagnostics() throws Exception {
    Path source = write("parser-error.idl", "component Handle;\n");

    CliRun result = run("validate", source.toString());

    assertEquals(IdljExitCodes.VALIDATION_FAILED, result.exitCode());
    assertEquals("", result.stdout());
    assertTrue(result.stderr().contains("ERROR IDL-0302: unsupported declaration: component"));
    assertTrue(result.stderr().startsWith(source + ":1:1: "));
  }

  @Test
  void returnsValidationFailureForSemanticDiagnostics() throws Exception {
    Path source =
        write("semantic-error.idl", "module Bad { struct Point { long x; short X; }; };\n");

    CliRun result = run("validate", source.toString());

    assertEquals(IdljExitCodes.VALIDATION_FAILED, result.exitCode());
    assertEquals("", result.stdout());
    assertTrue(result.stderr().contains("ERROR IDL-0400: Duplicate IDL name in scope: X"));
  }

  @Test
  void reportsUsageAndInputFailuresWithStableExitCode() throws Exception {
    Path missing = tempDir.resolve("missing.idl");
    Path directory = Files.createDirectory(tempDir.resolve("directory.idl"));

    assertUsageError(run());
    assertUsageError(run("compile"));
    assertUsageError(run("validate", "--unknown", "demo.idl"));
    assertUsageError(run("validate", "-I"));
    assertUsageError(run("validate", "--include"));
    assertUsageError(run("validate"));

    CliRun missingFile = run("validate", missing.toString());
    assertEquals(IdljExitCodes.USAGE_OR_INPUT_ERROR, missingFile.exitCode());
    assertEquals("", missingFile.stdout());
    assertTrue(missingFile.stderr().contains("ERROR IDLJ-0002: Could not read IDL source file"));

    CliRun directoryInput = run("validate", "--quiet", directory.toString());
    assertEquals(IdljExitCodes.USAGE_OR_INPUT_ERROR, directoryInput.exitCode());
    assertEquals("", directoryInput.stdout());
    assertTrue(directoryInput.stderr().contains("ERROR IDLJ-0002: Could not read IDL source file"));
  }

  @Test
  void returnsValidationFailureForFilesystemIncludeDiagnostics() throws Exception {
    Path source =
        write(
            "missing-include.idl",
            """
            #include "missing.idl"
            module Demo { const long VALUE = 1; };
            """);

    CliRun result = run("validate", "--quiet", "-I", tempDir.toString(), source.toString());

    assertEquals(IdljExitCodes.VALIDATION_FAILED, result.exitCode());
    assertEquals("", result.stdout());
    assertTrue(result.stderr().contains("ERROR IDL-0202:"));
    assertTrue(result.stderr().contains("missing.idl"));
    assertTrue(result.stderr().startsWith(source + ":1:1: "));
  }

  @Test
  void reportsDiagnosticsAtLineMarkerRemappedSourceLocations() throws Exception {
    Path source =
        write(
            "line-marker.idl",
            """
            #line 88 "original.idl"
            module Bad { struct Point { long x; short X; }; };
            """);

    CliRun result = run("validate", source.toString());

    assertEquals(IdljExitCodes.VALIDATION_FAILED, result.exitCode());
    assertEquals("", result.stdout());
    assertTrue(result.stderr().contains("original.idl:88:37: ERROR IDL-0400"), result.stderr());
  }

  @Test
  void preservesFileAndDiagnosticEncounterOrder() throws Exception {
    Path parserError = write("a-parser.idl", "component Handle;\n");
    Path semanticError =
        write("b-semantic.idl", "module Bad { struct Point { long x; short X; }; };\n");

    CliRun result = run("validate", parserError.toString(), semanticError.toString());

    assertEquals(IdljExitCodes.VALIDATION_FAILED, result.exitCode());
    assertTrue(result.stderr().indexOf(parserError.toString()) >= 0);
    assertTrue(result.stderr().indexOf(semanticError.toString()) >= 0);
    assertTrue(
        result.stderr().indexOf(parserError.toString()) < result.stderr().indexOf("IDL-0400"));
  }

  @Test
  void validatesG10GrammarClosureSliceWithoutGeneration() throws Exception {
    Path source =
        write(
            "g10.idl",
            """
            module G10 {
              const unsigned long LIMIT = 4;
              interface Forward;
              typedef sequence<string<32>, LIMIT> Names;
              typedef long Matrix[2][LIMIT], Count;
              union Choice switch (long) {
                case 0:
                case 1: string<16> text;
                default: Names names;
              };
              exception Problem { string reason; };
              interface Base { void ping(); };
              interface Service : Base, Forward {
                attribute Count counts[LIMIT];
                void submit(in Names names, out Choice result, inout Count count) raises (Problem);
                void collect(out sequence<string<32>> values);
              };
            };
            """);

    assertQuietSuccess(run("validate", "--quiet", source.toString()));
  }

  @Test
  void validatesG12GrammarHardeningCorpusWithoutGeneration() throws Exception {
    Path source =
        write(
            "g12.idl",
            """
            #pragma prefix "example.com"
            module G12 {
              native Handle;
              exception Problem {};
              abstract interface AbstractBase;
              local interface LocalControl { void ping() context ("tenant", "trace"); };
              valuetype NameValue string<32>;
              valuetype ForwardValue;
              valuetype Holder : ForwardValue supports AbstractBase {
                public long id;
                private sequence<string, 8> names;
                factory create(in long id) raises (Problem);
                void touch(in Handle handle);
              };
              typeid Holder "IDL:example.com/G12/Holder:1.0";
              typeprefix Holder "example.com/G12";
            };
            """);

    assertQuietSuccess(run("validate", "--quiet", source.toString()));
  }

  @Test
  void rejectsMalformedG12GrammarFixtureWithoutGeneration() throws Exception {
    Path source = write("bad-g12.idl", "valuetype Bad { factory create(out long id); };\n");

    CliRun result = run("validate", "--quiet", source.toString());

    assertEquals(IdljExitCodes.VALIDATION_FAILED, result.exitCode());
    assertEquals("", result.stdout());
    assertTrue(result.stderr().contains("ERROR IDL-0300: Expected in factory parameter direction"));
  }

  @Test
  void canRunRepeatedlyWithoutLeakingState() throws Exception {
    Path source = write("repeat.idl", "module Repeat { const long VALUE = 3; };\n");
    IdljCli cli = new IdljCli();

    CliRun first = run(cli, "validate", "--quiet", source.toString());
    CliRun second = run(cli, "validate", "--quiet", source.toString());

    assertEquals(first, second);
    assertEquals(IdljExitCodes.SUCCESS, first.exitCode());
  }

  @Test
  void validatesSharedHelloFixtureWithoutGeneration() {
    Path hello = findRepositoryRoot().resolve("interop/idl/hello/hello.idl");

    CliRun result = run("validate", "--quiet", hello.toString());

    assertQuietSuccess(result);
  }

  @Test
  void diagnosticFormatterRendersStableSpanAndNoSpanForms() {
    IdljDiagnosticFormatter formatter = new IdljDiagnosticFormatter();
    DiagnosticCode code = new DiagnosticCode("TEST-1234");
    SourcePosition start = new SourcePosition("demo.idl", 4, 7, 30);
    SourceSpan span = new SourceSpan(start, new SourcePosition("demo.idl", 4, 11, 34));

    assertEquals(
        "ERROR TEST-1234: no source",
        formatter.format(Diagnostic.withoutSpan(code, DiagnosticSeverity.ERROR, "no source")));
    assertEquals(
        "demo.idl:4:7: WARNING TEST-1234: with source",
        formatter.format(
            Diagnostic.withSpan(code, DiagnosticSeverity.WARNING, "with source", span)));
    assertThrows(NullPointerException.class, () -> formatter.format(null));
  }

  private Path write(String name, String source) throws Exception {
    Path path = tempDir.resolve(name);
    Files.writeString(path, source, StandardCharsets.UTF_8);
    return path;
  }

  private static void assertQuietSuccess(CliRun result) {
    assertEquals(IdljExitCodes.SUCCESS, result.exitCode());
    assertEquals("", result.stdout());
    assertEquals("", result.stderr());
  }

  private static void assertUsageError(CliRun result) {
    assertEquals(IdljExitCodes.USAGE_OR_INPUT_ERROR, result.exitCode());
    assertEquals("", result.stdout());
    assertTrue(result.stderr().contains("ERROR IDLJ-0001: "));
  }

  private static CliRun run(String... args) {
    return run(new IdljCli(), args);
  }

  private static CliRun run(IdljCli cli, String... args) {
    StringWriter stdout = new StringWriter();
    StringWriter stderr = new StringWriter();
    int exitCode = cli.run(args, new PrintWriter(stdout, true), new PrintWriter(stderr, true));
    return new CliRun(exitCode, stdout.toString(), stderr.toString());
  }

  private static Path findRepositoryRoot() {
    Path directory = Path.of("").toAbsolutePath().normalize();
    while (directory != null) {
      if (Files.isRegularFile(directory.resolve("AGENT.md"))
          && Files.isDirectory(directory.resolve("modules"))) {
        return directory;
      }
      directory = directory.getParent();
    }
    throw new IllegalStateException("Could not locate repository root from test working directory");
  }

  private record CliRun(int exitCode, String stdout, String stderr) {}
}
