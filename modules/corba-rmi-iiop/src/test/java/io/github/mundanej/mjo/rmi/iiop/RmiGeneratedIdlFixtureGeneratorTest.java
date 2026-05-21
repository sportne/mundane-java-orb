package io.github.mundanej.mjo.rmi.iiop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RmiGeneratedIdlFixtureGenerator}. */
@Tag("unit")
final class RmiGeneratedIdlFixtureGeneratorTest {

  private static final RmiJavaTypeReference REMOTE_EXCEPTION =
      RmiJavaTypeReference.declared("java.rmi.RemoteException");

  private final RmiJavaToIdlMapper mapper = new RmiJavaToIdlMapper();
  private final RmiGeneratedIdlFixtureGenerator generator = new RmiGeneratedIdlFixtureGenerator();

  @Test
  void generatesDeterministicGoldenIdlFixtureFromApprovedModel() {
    RmiIdlTranslationUnit translationUnit = approvedTranslationUnit();

    RmiGeneratedIdlResult result = generator.generate(translationUnit);
    RmiGeneratedIdlResult repeated = generator.generate(translationUnit);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    assertEquals(List.of(), result.diagnostics());
    assertEquals(result, repeated);
    assertEquals("rmi-generated.idl", result.fixture().orElseThrow().sourceName());
    assertEquals(goldenFixture(), result.fixture().orElseThrow().idlText());
  }

  @Test
  void generatedIdlMatchesRmiIiopPeerScenarioFixture() {
    RmiGeneratedIdlResult result = generator.generate(approvedTranslationUnit());

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    assertEquals(peerScenarioFixture(), result.fixture().orElseThrow().idlText());
  }

  @Test
  void reportsUnsupportedGeneratedIdlInputsInModelOrder() {
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

    RmiGeneratedIdlResult result = generator.generate(translationUnit);

    assertTrue(result.hasErrors());
    assertTrue(result.fixture().isEmpty());
    assertEquals(
        List.of(
            RmiJavaDiagnosticCodes.UNSUPPORTED_GENERATED_IDL_SEQUENCE,
            RmiJavaDiagnosticCodes.UNSUPPORTED_GENERATED_IDL_DECLARED_TYPE,
            RmiJavaDiagnosticCodes.UNSUPPORTED_GENERATED_IDL_EXCEPTION_SCOPE),
        diagnosticCodes(result));
  }

  @Test
  void exposesImmutableGeneratedFixtureResults() {
    RmiGeneratedIdlResult result = generator.generate(approvedTranslationUnit());
    RmiGeneratedIdlFixture fixture = result.fixture().orElseThrow();

    assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
    assertThrows(IllegalArgumentException.class, () -> new RmiGeneratedIdlFixture(" ", "x\n"));
    assertThrows(
        IllegalArgumentException.class, () -> new RmiGeneratedIdlFixture("bad.idl", "no-newline"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RmiGeneratedIdlResult(Optional.of(fixture), resultWithErrorCodes()));
  }

  @Test
  void keepsGeneratedIdlDiagnosticCodeValuesStable() {
    assertEquals(
        List.of("RMI-0400", "RMI-0401", "RMI-0402"),
        List.of(
                RmiJavaDiagnosticCodes.UNSUPPORTED_GENERATED_IDL_SEQUENCE,
                RmiJavaDiagnosticCodes.UNSUPPORTED_GENERATED_IDL_DECLARED_TYPE,
                RmiJavaDiagnosticCodes.UNSUPPORTED_GENERATED_IDL_EXCEPTION_SCOPE)
            .stream()
            .map(DiagnosticCode::value)
            .toList());
  }

  @Test
  void mainSourcesAvoidForbiddenRuntimeAndBindingMechanisms() throws Exception {
    Path sourceRoot = Path.of("src/main/java");
    try (Stream<Path> paths = Files.walk(sourceRoot)) {
      String sources =
          paths
              .filter(path -> path.toString().endsWith(".java"))
              .map(RmiGeneratedIdlFixtureGeneratorTest::readString)
              .reduce("", String::concat);

      assertFalse(sources.contains("Class.forName"));
      assertFalse(sources.contains("java.lang.reflect"));
      assertFalse(sources.contains("Proxy.newProxyInstance"));
      assertFalse(sources.contains("ObjectInputStream"));
      assertFalse(sources.contains("ObjectOutputStream"));
      assertFalse(sources.contains("java.io.Serializable"));
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

  private static List<DiagnosticCode> diagnosticCodes(RmiGeneratedIdlResult result) {
    assertTrue(
        result.diagnostics().stream()
            .allMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR));
    return result.diagnostics().stream().map(Diagnostic::code).toList();
  }

  private static List<Diagnostic> resultWithErrorCodes() {
    return List.of(
        Diagnostic.withoutSpan(
            RmiJavaDiagnosticCodes.UNSUPPORTED_GENERATED_IDL_SEQUENCE,
            DiagnosticSeverity.ERROR,
            "error"));
  }

  private static String goldenFixture() {
    return readString(Path.of("src/test/resources/rmi-generated-idl/calculator.idl"));
  }

  private static String peerScenarioFixture() {
    return readString(
        Path.of("../..")
            .toAbsolutePath()
            .normalize()
            .resolve("interop/idl/rmi-iiop/Calculator.idl"));
  }

  private static String readString(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to read " + path, exception);
    }
  }
}
