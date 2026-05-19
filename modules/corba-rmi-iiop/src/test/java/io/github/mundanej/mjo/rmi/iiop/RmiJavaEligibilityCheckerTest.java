package io.github.mundanej.mjo.rmi.iiop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RmiJavaEligibilityChecker}. */
@Tag("unit")
final class RmiJavaEligibilityCheckerTest {

  private static final RmiJavaTypeReference REMOTE_EXCEPTION =
      RmiJavaTypeReference.declared("java.rmi.RemoteException");

  private final RmiJavaEligibilityChecker checker = new RmiJavaEligibilityChecker();

  @Test
  void acceptsExplicitRemoteInterfaceDeclaration() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "example.Calculator",
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
                            "target", RmiJavaTypeReference.declared("example.Target"))),
                    List.of(
                        RmiJavaTypeReference.declared("example.CheckedProblem"), REMOTE_EXCEPTION)),
                RmiJavaOperation.abstractOperation(
                    "clear",
                    RmiJavaTypeReference.voidType(),
                    List.of(),
                    List.of(REMOTE_EXCEPTION))));

    RmiJavaEligibilityResult result = checker.check(declaration);
    RmiJavaEligibilityResult repeated = checker.check(declaration);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    assertEquals(Optional.of(declaration), result.remoteInterface());
    assertEquals(List.of(), result.diagnostics());
    assertEquals(result, repeated);
  }

  @Test
  void rejectsNullAndInvalidRemoteInterfaceInputsWithStableDiagnostics() {
    assertEquals(
        List.of(RmiJavaDiagnosticCodes.NULL_DECLARATION), diagnosticCodes(checker.check(null)));

    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "example.1Bad",
            false,
            List.of(
                RmiJavaOperation.abstractOperation(
                    "ping",
                    RmiJavaTypeReference.voidType(),
                    List.of(),
                    List.of(REMOTE_EXCEPTION))));

    RmiJavaEligibilityResult result = checker.check(declaration);

    assertTrue(result.hasErrors());
    assertTrue(result.remoteInterface().isEmpty());
    assertEquals(
        List.of(
            RmiJavaDiagnosticCodes.INVALID_INTERFACE_NAME,
            RmiJavaDiagnosticCodes.NON_REMOTE_INTERFACE),
        diagnosticCodes(result));
  }

  @Test
  void rejectsUnsupportedMethodShapesAndPreservesEncounterOrder() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "example.BadMethods",
            true,
            List.of(
                new RmiJavaOperation(
                    "defaultName",
                    RmiJavaTypeReference.voidType(),
                    List.of(),
                    List.of(REMOTE_EXCEPTION),
                    RmiJavaOperationKind.DEFAULT,
                    true),
                RmiJavaOperation.abstractOperation(
                    "same", RmiJavaTypeReference.voidType(), List.of(), List.of(REMOTE_EXCEPTION)),
                RmiJavaOperation.abstractOperation(
                    "same",
                    RmiJavaTypeReference.voidType(),
                    List.of(),
                    List.of(REMOTE_EXCEPTION))));

    assertEquals(
        List.of(
            RmiJavaDiagnosticCodes.UNSUPPORTED_OPERATION_KIND,
            RmiJavaDiagnosticCodes.UNSUPPORTED_VARARGS,
            RmiJavaDiagnosticCodes.DUPLICATE_OPERATION_NAME),
        diagnosticCodes(checker.check(declaration)));
  }

  @Test
  void rejectsUnsupportedTypeReferencesAndMissingRemoteException() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "example.BadTypes",
            true,
            List.of(
                RmiJavaOperation.abstractOperation(
                    "bad",
                    RmiJavaTypeReference.array("int[]"),
                    List.of(
                        new RmiJavaParameter(
                            "values",
                            RmiJavaTypeReference.generic("java.util.List<java.lang.String>")),
                        new RmiJavaParameter("anything", RmiJavaTypeReference.wildcard("?")),
                        new RmiJavaParameter("none", RmiJavaTypeReference.voidType())),
                    List.of(RmiJavaTypeReference.array("java.lang.Exception[]")))));

    assertEquals(
        List.of(
            RmiJavaDiagnosticCodes.UNSUPPORTED_TYPE_REFERENCE,
            RmiJavaDiagnosticCodes.UNSUPPORTED_TYPE_REFERENCE,
            RmiJavaDiagnosticCodes.UNSUPPORTED_TYPE_REFERENCE,
            RmiJavaDiagnosticCodes.UNSUPPORTED_TYPE_REFERENCE,
            RmiJavaDiagnosticCodes.INVALID_EXCEPTION_TYPE,
            RmiJavaDiagnosticCodes.MISSING_REMOTE_EXCEPTION),
        diagnosticCodes(checker.check(declaration)));
  }

  @Test
  void rejectsBlankReservedAndMalformedNames() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            " ",
            true,
            List.of(
                RmiJavaOperation.abstractOperation(
                    "class",
                    RmiJavaTypeReference.declared("bad-name"),
                    List.of(new RmiJavaParameter("return", RmiJavaTypeReference.primitive("int"))),
                    List.of(REMOTE_EXCEPTION))));

    assertEquals(
        List.of(
            RmiJavaDiagnosticCodes.INVALID_INTERFACE_NAME,
            RmiJavaDiagnosticCodes.INVALID_OPERATION_NAME,
            RmiJavaDiagnosticCodes.UNSUPPORTED_TYPE_REFERENCE,
            RmiJavaDiagnosticCodes.INVALID_PARAMETER_NAME),
        diagnosticCodes(checker.check(declaration)));
  }

  @Test
  void exposesImmutableResultAndModelValues() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "example.Immutable",
            true,
            List.of(
                RmiJavaOperation.abstractOperation(
                    "ping",
                    RmiJavaTypeReference.voidType(),
                    List.of(),
                    List.of(REMOTE_EXCEPTION))));
    RmiJavaEligibilityResult result = checker.check(declaration);

    assertThrows(UnsupportedOperationException.class, () -> declaration.operations().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
    assertThrows(
        IllegalArgumentException.class,
        () -> new RmiJavaEligibilityResult(Optional.of(declaration), resultWithErrorCodes()));
  }

  @Test
  void keepsDiagnosticCodeValuesStable() {
    assertEquals(
        List.of(
            "RMI-0100",
            "RMI-0101",
            "RMI-0102",
            "RMI-0103",
            "RMI-0104",
            "RMI-0105",
            "RMI-0106",
            "RMI-0107",
            "RMI-0108",
            "RMI-0109",
            "RMI-0110"),
        List.of(
                RmiJavaDiagnosticCodes.NULL_DECLARATION,
                RmiJavaDiagnosticCodes.INVALID_INTERFACE_NAME,
                RmiJavaDiagnosticCodes.NON_REMOTE_INTERFACE,
                RmiJavaDiagnosticCodes.INVALID_OPERATION_NAME,
                RmiJavaDiagnosticCodes.UNSUPPORTED_OPERATION_KIND,
                RmiJavaDiagnosticCodes.UNSUPPORTED_VARARGS,
                RmiJavaDiagnosticCodes.DUPLICATE_OPERATION_NAME,
                RmiJavaDiagnosticCodes.UNSUPPORTED_TYPE_REFERENCE,
                RmiJavaDiagnosticCodes.MISSING_REMOTE_EXCEPTION,
                RmiJavaDiagnosticCodes.INVALID_PARAMETER_NAME,
                RmiJavaDiagnosticCodes.INVALID_EXCEPTION_TYPE)
            .stream()
            .map(DiagnosticCode::value)
            .toList());
  }

  @Test
  void mainSourcesDoNotUseForbiddenRuntimeInspectionMechanisms() throws Exception {
    Path sourceRoot = Path.of("src/main/java");
    try (Stream<Path> paths = Files.walk(sourceRoot)) {
      String sources =
          paths
              .filter(path -> path.toString().endsWith(".java"))
              .map(RmiJavaEligibilityCheckerTest::readString)
              .reduce("", String::concat);

      assertFalse(sources.contains("Class.forName"));
      assertFalse(sources.contains("java.lang.reflect"));
      assertFalse(sources.contains("Proxy.newProxyInstance"));
      assertFalse(sources.contains("ServiceLoader"));
      assertFalse(sources.contains("ObjectInputStream"));
      assertFalse(sources.contains("ObjectOutputStream"));
      assertFalse(sources.contains("java.io.Serializable"));
    }
  }

  private static List<DiagnosticCode> diagnosticCodes(RmiJavaEligibilityResult result) {
    assertTrue(
        result.diagnostics().stream()
            .allMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR));
    return result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList();
  }

  private static List<io.github.mundanej.mjo.common.Diagnostic> resultWithErrorCodes() {
    return List.of(
        io.github.mundanej.mjo.common.Diagnostic.withoutSpan(
            RmiJavaDiagnosticCodes.NON_REMOTE_INTERFACE, DiagnosticSeverity.ERROR, "forced error"));
  }

  private static String readString(Path path) {
    try {
      return Files.readString(path);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Unable to read " + path, exception);
    }
  }
}
