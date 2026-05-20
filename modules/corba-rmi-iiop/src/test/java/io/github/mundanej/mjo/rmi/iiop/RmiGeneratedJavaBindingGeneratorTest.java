package io.github.mundanej.mjo.rmi.iiop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link RmiGeneratedJavaBindingGenerator}. */
@Tag("unit")
@Tag("generated-code")
final class RmiGeneratedJavaBindingGeneratorTest {

  private static final RmiJavaTypeReference REMOTE_EXCEPTION =
      RmiJavaTypeReference.declared("java.rmi.RemoteException");
  private static final List<String> FORBIDDEN_RUNTIME_TOKENS =
      List.of(
          "Class.forName",
          "java.lang.reflect",
          "Proxy.newProxyInstance",
          "ObjectInputStream",
          "ObjectOutputStream",
          "java.io.Serializable",
          "io.github.mundanej.mjo.orb",
          "io.github.mundanej.mjo.iiop",
          "ServiceLoader",
          "ClassLoader");

  private final RmiJavaToIdlMapper mapper = new RmiJavaToIdlMapper();
  private final RmiGeneratedJavaBindingGenerator generator = new RmiGeneratedJavaBindingGenerator();

  @TempDir private Path tempDir;

  @Test
  void generatesDeterministicCompileSafeBindingSources() throws Exception {
    RmiIdlTranslationUnit translationUnit = approvedTranslationUnit();
    RmiRepositoryIdPlan repositoryIdPlan = approvedRepositoryIdPlan();

    RmiGeneratedJavaBindingResult result = generator.generate(translationUnit, repositoryIdPlan);
    RmiGeneratedJavaBindingResult repeated = generator.generate(translationUnit, repositoryIdPlan);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    assertEquals(List.of(), result.diagnostics());
    assertEquals(result, repeated);
    assertEquals(
        List.of(
            "example/calc/Calculator.java",
            "example/calc/CalculatorBindingDescriptor.java",
            "example/calc/CalculatorHelper.java",
            "example/calc/CalculatorHolder.java",
            "example/calc/CalculatorProblem.java",
            "example/calc/CalculatorProblemHolder.java",
            "example/calc/CalculatorSkeleton.java",
            "example/calc/CalculatorStub.java",
            "example/calc/CalculatorTie.java"),
        result.sources().stream().map(RmiGeneratedJavaBindingSource::sourcePath).toList());
    assertEquals(goldenCalculatorInterface(), sourceText(result, "example/calc/Calculator.java"));
    assertEquals(goldenCalculatorStub(), sourceText(result, "example/calc/CalculatorStub.java"));
    assertTrue(
        sourceText(result, "example/calc/CalculatorHelper.java")
            .contains("RMI:example.calc.Calculator:0123456789ABCDEF"));
    assertTrue(
        sourceText(result, "example/calc/CalculatorProblem.java")
            .contains("RMI:example.calc.CalculatorProblem:2222222222222222"));
    assertNoForbiddenRuntimeTokens(result);
    compile(result.sources());
  }

  @Test
  void reportsUnsupportedBindingInputsInModelOrder() {
    RmiIdlTranslationUnit translationUnit =
        new RmiIdlTranslationUnit(
            List.of(),
            List.of(
                new RmiIdlInterface(
                    "Unsupported",
                    "::Unsupported",
                    Optional.of("example.Unsupported"),
                    List.of(
                        new RmiIdlOperation(
                            "bad",
                            RmiIdlTypeReference.sequenceOf(RmiIdlTypeReference.builtin("long")),
                            List.of(
                                new RmiIdlParameter(
                                    "value",
                                    RmiIdlTypeReference.declaredValue(
                                        "::example::Value", "example.Value"))),
                            List.of(
                                new RmiIdlExceptionReference(
                                    "other.Problem", "::other::Problem")))))));

    RmiGeneratedJavaBindingResult result =
        generator.generate(
            translationUnit,
            new RmiRepositoryIdPlan(
                List.of(
                    new RmiRepositoryIdValue(
                        "example.Unsupported", "RMI:example.Unsupported:0123456789ABCDEF"))));

    assertTrue(result.hasErrors());
    assertEquals(List.of(), result.sources());
    assertEquals(
        List.of(
            RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_SEQUENCE,
            RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_DECLARED_TYPE,
            RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_EXCEPTION_SCOPE,
            RmiJavaDiagnosticCodes.MISSING_BINDING_REPOSITORY_ID),
        diagnosticCodes(result));
  }

  @Test
  void reportsMissingRepositoryIdsAndDuplicateSourcePaths() {
    RmiIdlTranslationUnit missingRepositoryId =
        new RmiIdlTranslationUnit(
            List.of(),
            List.of(
                new RmiIdlInterface(
                    "Missing", "::Missing", Optional.of("example.Missing"), List.of())));
    RmiIdlTranslationUnit duplicateSourcePath =
        new RmiIdlTranslationUnit(
            List.of(),
            List.of(
                new RmiIdlInterface(
                    "Duplicate",
                    "::Duplicate",
                    Optional.of("example.Duplicate"),
                    List.of(
                        new RmiIdlOperation(
                            "bad",
                            RmiIdlTypeReference.voidType(),
                            List.of(),
                            List.of(
                                new RmiIdlExceptionReference(
                                    "example.DuplicateProblem", "::Duplicate")))))));

    assertEquals(
        List.of(RmiJavaDiagnosticCodes.MISSING_BINDING_REPOSITORY_ID),
        diagnosticCodes(
            generator.generate(missingRepositoryId, new RmiRepositoryIdPlan(List.of()))));
    assertEquals(
        List.of(
            RmiJavaDiagnosticCodes.DUPLICATE_BINDING_SOURCE_PATH,
            RmiJavaDiagnosticCodes.DUPLICATE_BINDING_SOURCE_PATH),
        diagnosticCodes(
            generator.generate(
                duplicateSourcePath,
                new RmiRepositoryIdPlan(
                    List.of(
                        new RmiRepositoryIdValue(
                            "example.Duplicate", "RMI:example.Duplicate:0123456789ABCDEF"),
                        new RmiRepositoryIdValue(
                            "example.DuplicateProblem",
                            "RMI:example.DuplicateProblem:2222222222222222"))))));
  }

  @Test
  void exposesImmutableBindingValues() {
    RmiGeneratedJavaBindingResult result =
        generator.generate(approvedTranslationUnit(), approvedRepositoryIdPlan());
    RmiGeneratedJavaBindingSource source = result.sources().getFirst();

    assertThrows(UnsupportedOperationException.class, () -> result.sources().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
    assertThrows(
        IllegalArgumentException.class,
        () -> new RmiGeneratedJavaBindingSource("example", "Thing", "class Thing {}"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RmiGeneratedJavaBindingResult(List.of(source), resultWithErrorCodes()));
  }

  @Test
  void keepsGeneratedBindingDiagnosticCodeValuesStable() {
    assertEquals(
        List.of("RMI-0500", "RMI-0501", "RMI-0502", "RMI-0503", "RMI-0504"),
        List.of(
                RmiJavaDiagnosticCodes.MISSING_BINDING_REPOSITORY_ID,
                RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_SEQUENCE,
                RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_DECLARED_TYPE,
                RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_EXCEPTION_SCOPE,
                RmiJavaDiagnosticCodes.DUPLICATE_BINDING_SOURCE_PATH)
            .stream()
            .map(DiagnosticCode::value)
            .toList());
  }

  @Test
  void mainSourcesAvoidForbiddenRuntimeMechanisms() throws Exception {
    Path sourceRoot = Path.of("src/main/java");
    try (Stream<Path> paths = Files.walk(sourceRoot)) {
      String sources =
          paths
              .filter(path -> path.toString().endsWith(".java"))
              .map(RmiGeneratedJavaBindingGeneratorTest::readString)
              .reduce("", String::concat);

      assertEquals(
          List.of(),
          FORBIDDEN_RUNTIME_TOKENS.stream().filter(sources::contains).toList(),
          "RMI-IIOP main sources contain forbidden runtime mechanisms");
    }
  }

  private RmiIdlTranslationUnit approvedTranslationUnit() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "example.calc.Calculator",
            true,
            List.of(
                RmiJavaOperation.abstractOperation(
                    "add",
                    RmiJavaTypeReference.primitive("int"),
                    List.of(
                        new RmiJavaParameter("left", RmiJavaTypeReference.primitive("int")),
                        new RmiJavaParameter("right", RmiJavaTypeReference.primitive("int"))),
                    List.of(REMOTE_EXCEPTION)),
                RmiJavaOperation.abstractOperation(
                    "describe",
                    RmiJavaTypeReference.declared("java.lang.String"),
                    List.of(
                        new RmiJavaParameter(
                            "name", RmiJavaTypeReference.declared("java.lang.String"))),
                    List.of(
                        RmiJavaTypeReference.declared("example.calc.CalculatorProblem"),
                        REMOTE_EXCEPTION)),
                RmiJavaOperation.abstractOperation(
                    "clear",
                    RmiJavaTypeReference.voidType(),
                    List.of(),
                    List.of(REMOTE_EXCEPTION))));

    RmiJavaToIdlResult result = mapper.map(declaration);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    return result.translationUnit().orElseThrow();
  }

  private static RmiRepositoryIdPlan approvedRepositoryIdPlan() {
    return new RmiRepositoryIdPlan(
        List.of(
            new RmiRepositoryIdValue(
                "example.calc.Calculator", "RMI:example.calc.Calculator:0123456789ABCDEF"),
            new RmiRepositoryIdValue(
                "example.calc.CalculatorProblem",
                "RMI:example.calc.CalculatorProblem:2222222222222222")));
  }

  private static String goldenCalculatorInterface() {
    return """
        // Generated by mundane-java-orb G7-050.
        // Compatibility profile: compile-safe RMI-IIOP binding surface.

        package example.calc;

        public interface Calculator extends java.rmi.Remote {

          public int add(int left, int right) throws java.rmi.RemoteException;

          public java.lang.String describe(java.lang.String name) throws java.rmi.RemoteException, CalculatorProblem;

          public void clear() throws java.rmi.RemoteException;
        }
        """;
  }

  private static String goldenCalculatorStub() {
    return """
        // Generated by mundane-java-orb G7-050.
        // Compatibility profile: compile-safe RMI-IIOP binding surface.

        package example.calc;

        public final class CalculatorStub implements Calculator {

          public static final String DEFERRED_INVOCATION_MESSAGE = "RMI-IIOP invocation is deferred to G7-070/G7-080.";

          @Override
          public int add(int left, int right) throws java.rmi.RemoteException {
            throw new UnsupportedOperationException(DEFERRED_INVOCATION_MESSAGE);
          }

          @Override
          public java.lang.String describe(java.lang.String name) throws java.rmi.RemoteException, CalculatorProblem {
            throw new UnsupportedOperationException(DEFERRED_INVOCATION_MESSAGE);
          }

          @Override
          public void clear() throws java.rmi.RemoteException {
            throw new UnsupportedOperationException(DEFERRED_INVOCATION_MESSAGE);
          }
        }
        """;
  }

  private static String sourceText(RmiGeneratedJavaBindingResult result, String sourcePath) {
    return result.sources().stream()
        .filter(source -> source.sourcePath().equals(sourcePath))
        .findFirst()
        .orElseThrow()
        .sourceText();
  }

  private static List<DiagnosticCode> diagnosticCodes(RmiGeneratedJavaBindingResult result) {
    assertTrue(
        result.diagnostics().stream()
            .allMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR));
    return result.diagnostics().stream().map(Diagnostic::code).toList();
  }

  private static List<Diagnostic> resultWithErrorCodes() {
    return List.of(
        Diagnostic.withoutSpan(
            RmiJavaDiagnosticCodes.MISSING_BINDING_REPOSITORY_ID,
            DiagnosticSeverity.ERROR,
            "error"));
  }

  private static void assertNoForbiddenRuntimeTokens(RmiGeneratedJavaBindingResult result) {
    String sourceText =
        result.sources().stream()
            .map(RmiGeneratedJavaBindingSource::sourceText)
            .reduce("", String::concat);
    assertEquals(
        List.of(),
        FORBIDDEN_RUNTIME_TOKENS.stream().filter(sourceText::contains).toList(),
        "Generated binding source contains forbidden runtime mechanisms");
  }

  private void compile(List<RmiGeneratedJavaBindingSource> sources) throws Exception {
    Path sourceRoot = Files.createDirectories(tempDir.resolve("generated-rmi-src"));
    Path classOutput = Files.createDirectories(tempDir.resolve("generated-rmi-classes"));
    List<String> arguments = new ArrayList<>();
    arguments.add("-d");
    arguments.add(classOutput.toString());
    for (RmiGeneratedJavaBindingSource source : sources) {
      Path sourceFile = sourceRoot.resolve(source.sourcePath());
      Path parent = sourceFile.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(sourceFile, source.sourceText(), StandardCharsets.UTF_8);
      arguments.add(sourceFile.toString());
    }

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler, "Generated-source compilation requires a JDK compiler");
    assertEquals(0, compiler.run(null, null, null, arguments.toArray(String[]::new)));
  }

  private static String readString(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to read " + path, exception);
    }
  }
}
