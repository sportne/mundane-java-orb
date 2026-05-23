package io.github.mundanej.mjo.rmi.iiop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RmiRepositoryIdPlanner}. */
@Tag("unit")
final class RmiRepositoryIdPlannerTest {

  private static final RmiJavaTypeReference REMOTE_EXCEPTION =
      RmiJavaTypeReference.declared("java.rmi.RemoteException");

  private final RmiJavaToIdlMapper mapper = new RmiJavaToIdlMapper();
  private final RmiRepositoryIdPlanner planner = new RmiRepositoryIdPlanner();

  @Test
  void plansDeterministicRepositoryIdsForInterfaceValuesAndExceptions() {
    RmiIdlTranslationUnit translationUnit = mappedTranslationUnit();

    RmiRepositoryIdPlanResult result =
        planner.plan(
            translationUnit,
            List.of(
                new RmiRepositoryIdHashMetadata("example.calc.Calculator", "0123456789abcdef"),
                new RmiRepositoryIdHashMetadata(
                    "example.calc.Target", "1111111111111111", "fedcba9876543210"),
                new RmiRepositoryIdHashMetadata(
                    "example.calc.CalculatorProblem", "2222222222222222")));

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    assertEquals(List.of(), result.diagnostics());
    assertEquals(
        List.of(
            new RmiRepositoryIdValue(
                "example.calc.Calculator", "RMI:example.calc.Calculator:0123456789ABCDEF"),
            new RmiRepositoryIdValue(
                "example.calc.Target", "RMI:example.calc.Target:1111111111111111:FEDCBA9876543210"),
            new RmiRepositoryIdValue(
                "example.calc.CalculatorProblem",
                "RMI:example.calc.CalculatorProblem:2222222222222222")),
        result.plan().orElseThrow().repositoryIds());
  }

  @Test
  void reportsMissingHashMetadataInModelTraversalOrder() {
    RmiRepositoryIdPlanResult result =
        planner.plan(
            mappedTranslationUnit(),
            List.of(
                new RmiRepositoryIdHashMetadata("example.calc.Calculator", "0123456789ABCDEF")));

    assertTrue(result.hasErrors());
    assertTrue(result.plan().isEmpty());
    assertEquals(
        List.of(
            RmiJavaDiagnosticCodes.MISSING_REPOSITORY_ID_HASH,
            RmiJavaDiagnosticCodes.MISSING_REPOSITORY_ID_HASH),
        diagnosticCodes(result));
    assertTrue(result.diagnostics().get(0).message().contains("example.calc.Target"));
    assertTrue(result.diagnostics().get(1).message().contains("example.calc.CalculatorProblem"));
  }

  @Test
  void deduplicatesRequiredNamesAndSkipsStringRepositoryIdsInTraversalOrder() {
    RmiIdlTranslationUnit translationUnit =
        new RmiIdlTranslationUnit(
            List.of(),
            List.of(
                new RmiIdlInterface(
                    "Root",
                    "::Root",
                    Optional.of("example.Root"),
                    List.of(
                        new RmiIdlOperation(
                            "lookup",
                            RmiIdlTypeReference.declaredValue("::example::Value", "example.Value"),
                            List.of(
                                new RmiIdlParameter(
                                    "sameValue",
                                    RmiIdlTypeReference.declaredValue(
                                        "::example::Value", "example.Value")),
                                new RmiIdlParameter(
                                    "names",
                                    RmiIdlTypeReference.sequenceOf(
                                        RmiIdlTypeReference.builtin("wstring")))),
                            List.of(
                                new RmiIdlExceptionReference(
                                    "example.Problem", "::example::Problem")))))));

    RmiRepositoryIdPlanResult result =
        planner.plan(
            translationUnit,
            List.of(
                new RmiRepositoryIdHashMetadata("example.Root", "aaaaaaaaaaaaaaaa"),
                new RmiRepositoryIdHashMetadata("example.Value", "bbbbbbbbbbbbbbbb"),
                new RmiRepositoryIdHashMetadata("example.Problem", "cccccccccccccccc"),
                new RmiRepositoryIdHashMetadata("java.lang.String", "dddddddddddddddd")));

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    assertEquals(
        List.of(
            "RMI:example.Root:AAAAAAAAAAAAAAAA",
            "RMI:example.Value:BBBBBBBBBBBBBBBB",
            "RMI:example.Problem:CCCCCCCCCCCCCCCC"),
        result.plan().orElseThrow().repositoryIds().stream()
            .map(RmiRepositoryIdValue::repositoryId)
            .toList());
  }

  @Test
  void reportsDuplicateInvalidAndUnresolvedInputsDeterministically() {
    RmiIdlTranslationUnit translationUnit =
        new RmiIdlTranslationUnit(
            List.of(),
            List.of(
                new RmiIdlInterface("MissingMetadata", "::MissingMetadata", List.of()),
                new RmiIdlInterface("BadName", "::BadName", Optional.of("bad-name"), List.of())));

    RmiRepositoryIdPlanResult result =
        planner.plan(
            translationUnit,
            List.of(
                new RmiRepositoryIdHashMetadata("bad-name", "not-hex", "also-bad"),
                new RmiRepositoryIdHashMetadata("example.Duplicate", "0123456789ABCDEF"),
                new RmiRepositoryIdHashMetadata("example.Duplicate", "FEDCBA9876543210")));

    assertTrue(result.hasErrors());
    assertTrue(result.plan().isEmpty());
    assertEquals(
        List.of(
            RmiJavaDiagnosticCodes.INVALID_REPOSITORY_ID_NAME,
            RmiJavaDiagnosticCodes.INVALID_REPOSITORY_ID_HASH,
            RmiJavaDiagnosticCodes.INVALID_REPOSITORY_ID_UID,
            RmiJavaDiagnosticCodes.DUPLICATE_REPOSITORY_ID_HASH,
            RmiJavaDiagnosticCodes.UNRESOLVED_REPOSITORY_ID_MODEL_NAME),
        diagnosticCodes(result));
  }

  @Test
  void exposesImmutablePlanningValues() {
    RmiRepositoryIdPlanResult result =
        planner.plan(
            new RmiIdlTranslationUnit(
                List.of(),
                List.of(
                    new RmiIdlInterface("Root", "::Root", Optional.of("example.Root"), List.of()))),
            List.of(new RmiRepositoryIdHashMetadata("example.Root", "0123456789ABCDEF")));
    RmiRepositoryIdPlan plan = result.plan().orElseThrow();

    assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
    assertThrows(UnsupportedOperationException.class, () -> plan.repositoryIds().clear());
    assertThrows(
        IllegalArgumentException.class,
        () -> new RmiRepositoryIdPlanResult(Optional.of(plan), resultWithErrorCodes()));
  }

  @Test
  void keepsRepositoryIdDiagnosticCodeValuesStable() {
    assertEquals(
        List.of("RMI-0300", "RMI-0301", "RMI-0302", "RMI-0303", "RMI-0304", "RMI-0305"),
        List.of(
                RmiJavaDiagnosticCodes.MISSING_REPOSITORY_ID_HASH,
                RmiJavaDiagnosticCodes.DUPLICATE_REPOSITORY_ID_HASH,
                RmiJavaDiagnosticCodes.INVALID_REPOSITORY_ID_NAME,
                RmiJavaDiagnosticCodes.INVALID_REPOSITORY_ID_HASH,
                RmiJavaDiagnosticCodes.INVALID_REPOSITORY_ID_UID,
                RmiJavaDiagnosticCodes.UNRESOLVED_REPOSITORY_ID_MODEL_NAME)
            .stream()
            .map(DiagnosticCode::value)
            .toList());
  }

  private RmiIdlTranslationUnit mappedTranslationUnit() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "example.calc.Calculator",
            true,
            List.of(
                RmiJavaOperation.abstractOperation(
                    "lookup",
                    RmiJavaTypeReference.declared("example.calc.Target"),
                    List.of(
                        new RmiJavaParameter(
                            "name", RmiJavaTypeReference.declared("java.lang.String"))),
                    List.of(
                        RmiJavaTypeReference.declared("example.calc.CalculatorProblem"),
                        REMOTE_EXCEPTION))));

    RmiJavaToIdlResult result = mapper.map(declaration);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    return result.translationUnit().orElseThrow();
  }

  private static List<DiagnosticCode> diagnosticCodes(RmiRepositoryIdPlanResult result) {
    assertTrue(
        result.diagnostics().stream()
            .allMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR));
    return result.diagnostics().stream().map(Diagnostic::code).toList();
  }

  private static List<Diagnostic> resultWithErrorCodes() {
    return List.of(
        Diagnostic.withoutSpan(
            RmiJavaDiagnosticCodes.MISSING_REPOSITORY_ID_HASH, DiagnosticSeverity.ERROR, "error"));
  }
}
