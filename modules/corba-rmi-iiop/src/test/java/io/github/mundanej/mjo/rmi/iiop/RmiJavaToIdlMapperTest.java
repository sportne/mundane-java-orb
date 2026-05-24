package io.github.mundanej.mjo.rmi.iiop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RmiJavaToIdlMapper}. */
@Tag("unit")
final class RmiJavaToIdlMapperTest {

  private static final RmiJavaTypeReference REMOTE_EXCEPTION =
      RmiJavaTypeReference.declared("java.rmi.RemoteException");

  private final RmiJavaToIdlMapper mapper = new RmiJavaToIdlMapper();

  @Test
  void mapsPackageInterfaceOperationsTypesAndExceptionsDeterministically() {
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
                        new RmiJavaParameter("right", RmiJavaTypeReference.primitive("long"))),
                    List.of(REMOTE_EXCEPTION)),
                RmiJavaOperation.abstractOperation(
                    "describe",
                    RmiJavaTypeReference.declared("java.lang.String"),
                    List.of(
                        new RmiJavaParameter(
                            "target", RmiJavaTypeReference.declared("example.calc.Target"))),
                    List.of(
                        RmiJavaTypeReference.declared("example.calc.CalculatorProblem"),
                        REMOTE_EXCEPTION)),
                RmiJavaOperation.abstractOperation(
                    "names",
                    RmiJavaTypeReference.arrayOf(RmiJavaTypeReference.declared("java.lang.String")),
                    List.of(
                        new RmiJavaParameter(
                            "ids",
                            RmiJavaTypeReference.arrayOf(RmiJavaTypeReference.primitive("int")))),
                    List.of(REMOTE_EXCEPTION))));

    RmiJavaToIdlResult result = mapper.map(declaration);
    RmiJavaToIdlResult repeated = mapper.map(declaration);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    assertEquals(List.of(), result.diagnostics());
    assertEquals(result, repeated);

    RmiIdlTranslationUnit translationUnit = result.translationUnit().orElseThrow();
    RmiIdlModule example = translationUnit.modules().getFirst();
    RmiIdlModule calc = example.modules().getFirst();
    RmiIdlInterface calculator = calc.interfaces().getFirst();

    assertEquals(List.of(), translationUnit.interfaces());
    assertEquals("example", example.name());
    assertEquals("::example", example.scopedName());
    assertEquals("calc", calc.name());
    assertEquals("::example::calc", calc.scopedName());
    assertEquals("Calculator", calculator.name());
    assertEquals("::example::calc::Calculator", calculator.scopedName());
    assertEquals(Optional.of("example.calc.Calculator"), calculator.javaBinaryName());

    RmiIdlOperation add = calculator.operations().get(0);
    assertEquals("add", add.name());
    assertEquals(RmiIdlTypeReference.builtin("long"), add.returnType());
    assertEquals(RmiIdlTypeReference.builtin("long"), add.parameters().get(0).type());
    assertEquals(RmiIdlTypeReference.builtin("long long"), add.parameters().get(1).type());
    assertEquals(List.of(), add.exceptions());

    RmiIdlOperation describe = calculator.operations().get(1);
    assertEquals(RmiIdlTypeReference.builtin("wstring"), describe.returnType());
    assertEquals(
        RmiIdlTypeReference.declaredValue("::example::calc::Target", "example.calc.Target"),
        describe.parameters().getFirst().type());
    assertEquals(
        List.of(
            new RmiIdlExceptionReference(
                "example.calc.CalculatorProblem", "::example::calc::CalculatorProblem")),
        describe.exceptions());

    RmiIdlOperation names = calculator.operations().get(2);
    assertEquals(RmiIdlTypeKind.SEQUENCE, names.returnType().kind());
    assertEquals(
        RmiIdlTypeReference.builtin("wstring"), names.returnType().elementType().orElseThrow());
    assertEquals(RmiIdlTypeKind.SEQUENCE, names.parameters().getFirst().type().kind());
    assertEquals(
        RmiIdlTypeReference.builtin("long"),
        names.parameters().getFirst().type().elementType().orElseThrow());
  }

  @Test
  void mapsDefaultPackageInterfaceAtTranslationUnitRoot() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "RootRemote",
            true,
            List.of(
                RmiJavaOperation.abstractOperation(
                    "ping",
                    RmiJavaTypeReference.voidType(),
                    List.of(),
                    List.of(REMOTE_EXCEPTION))));

    RmiIdlTranslationUnit translationUnit = mapper.map(declaration).translationUnit().orElseThrow();

    assertEquals(List.of(), translationUnit.modules());
    assertEquals("RootRemote", translationUnit.interfaces().getFirst().name());
    assertEquals("::RootRemote", translationUnit.interfaces().getFirst().scopedName());
  }

  @Test
  void mapsPrimitiveArrayAndDeclaredValueEdgesDeterministically() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "example.EdgeRemote",
            true,
            List.of(
                RmiJavaOperation.abstractOperation(
                    "edges",
                    RmiJavaTypeReference.arrayOf(
                        RmiJavaTypeReference.arrayOf(RmiJavaTypeReference.primitive("byte"))),
                    List.of(
                        new RmiJavaParameter("flag", RmiJavaTypeReference.primitive("boolean")),
                        new RmiJavaParameter("letter", RmiJavaTypeReference.primitive("char")),
                        new RmiJavaParameter("ratio", RmiJavaTypeReference.primitive("float")),
                        new RmiJavaParameter("total", RmiJavaTypeReference.primitive("double")),
                        new RmiJavaParameter(
                            "value", RmiJavaTypeReference.declared("example.Value"))),
                    List.of(REMOTE_EXCEPTION))));

    RmiJavaToIdlResult result = mapper.map(declaration);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    RmiIdlOperation operation =
        result
            .translationUnit()
            .orElseThrow()
            .modules()
            .getFirst()
            .interfaces()
            .getFirst()
            .operations()
            .getFirst();
    assertEquals(RmiIdlTypeKind.SEQUENCE, operation.returnType().kind());
    assertEquals(
        RmiIdlTypeKind.SEQUENCE, operation.returnType().elementType().orElseThrow().kind());
    assertEquals(
        RmiIdlTypeReference.builtin("octet"),
        operation.returnType().elementType().orElseThrow().elementType().orElseThrow());
    assertEquals(
        List.of(
            RmiIdlTypeReference.builtin("boolean"),
            RmiIdlTypeReference.builtin("wchar"),
            RmiIdlTypeReference.builtin("float"),
            RmiIdlTypeReference.builtin("double"),
            RmiIdlTypeReference.declaredValue("::example::Value", "example.Value")),
        operation.parameters().stream().map(RmiIdlParameter::type).toList());
  }

  @Test
  void mapsRemoteObjectReferencesAndInheritanceMetadata() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "example.DerivedRemote",
            true,
            List.of(
                RmiJavaOperation.abstractOperation(
                    "peer",
                    RmiJavaTypeReference.remote("example.BaseRemote"),
                    List.of(
                        new RmiJavaParameter(
                            "target", RmiJavaTypeReference.remote("example.BaseRemote"))),
                    List.of(REMOTE_EXCEPTION))),
            List.of(RmiJavaTypeReference.remote("example.BaseRemote")));

    RmiJavaToIdlResult result = mapper.map(declaration);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    RmiIdlInterface idlInterface =
        result.translationUnit().orElseThrow().modules().getFirst().interfaces().getFirst();
    RmiIdlOperation operation = idlInterface.operations().getFirst();
    assertEquals(List.of("::example::BaseRemote"), idlInterface.baseScopedNames());
    assertEquals(
        RmiIdlTypeReference.remoteObject("::example::BaseRemote", "example.BaseRemote"),
        operation.returnType());
    assertEquals(
        RmiIdlTypeReference.remoteObject("::example::BaseRemote", "example.BaseRemote"),
        operation.parameters().getFirst().type());
  }

  @Test
  void modelTypeReferencesValidateIncompatibleMetadata() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RmiJavaTypeReference(
                RmiJavaTypeKind.DECLARED,
                "example.Value",
                Optional.of(RmiJavaTypeReference.primitive("int"))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RmiIdlTypeReference(
                RmiIdlTypeKind.BUILTIN, " ", Optional.empty(), Optional.empty()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RmiIdlTypeReference(
                RmiIdlTypeKind.BUILTIN, "long", Optional.of("example.Value"), Optional.empty()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RmiIdlTypeReference(
                RmiIdlTypeKind.DECLARED_VALUE,
                "::example::Value",
                Optional.of("example.Value"),
                Optional.of(RmiIdlTypeReference.builtin("long"))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RmiIdlTypeReference(
                RmiIdlTypeKind.REMOTE_OBJECT,
                "::example::Remote",
                Optional.empty(),
                Optional.empty()));
  }

  @Test
  void propagatesEligibilityDiagnosticsWithoutCreatingModel() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "example.Bad",
            false,
            List.of(
                RmiJavaOperation.abstractOperation(
                    "ping",
                    RmiJavaTypeReference.voidType(),
                    List.of(),
                    List.of(REMOTE_EXCEPTION))));

    RmiJavaToIdlResult result = mapper.map(declaration);

    assertTrue(result.hasErrors());
    assertTrue(result.translationUnit().isEmpty());
    assertEquals(List.of(RmiJavaDiagnosticCodes.NON_REMOTE_INTERFACE), diagnosticCodes(result));
  }

  @Test
  void reportsMappingDiagnosticsForJavaNamesThatAreNotIdlNames() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "module.Bad$Remote",
            true,
            List.of(
                RmiJavaOperation.abstractOperation(
                    "bad$name",
                    RmiJavaTypeReference.declared("example.Bad$Value"),
                    List.of(
                        new RmiJavaParameter(
                            "bad$name", RmiJavaTypeReference.declared("example.GoodValue"))),
                    List.of(
                        RmiJavaTypeReference.declared("example.Bad$Problem"), REMOTE_EXCEPTION))));

    assertEquals(
        List.of(
            RmiJavaDiagnosticCodes.INVALID_IDL_MODULE_NAME,
            RmiJavaDiagnosticCodes.INVALID_IDL_INTERFACE_NAME,
            RmiJavaDiagnosticCodes.INVALID_IDL_OPERATION_NAME,
            RmiJavaDiagnosticCodes.UNSUPPORTED_IDL_TYPE_MAPPING,
            RmiJavaDiagnosticCodes.INVALID_IDL_PARAMETER_NAME,
            RmiJavaDiagnosticCodes.INVALID_IDL_EXCEPTION_NAME),
        diagnosticCodes(mapper.map(declaration)));
  }

  @Test
  void reportsMappingDiagnosticsForUnsupportedGenericAndWildcardShapes() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "example.Unsupported",
            true,
            List.of(
                RmiJavaOperation.abstractOperation(
                    "bad",
                    RmiJavaTypeReference.generic("java.util.List<java.lang.String>"),
                    List.of(new RmiJavaParameter("value", RmiJavaTypeReference.wildcard("?"))),
                    List.of(REMOTE_EXCEPTION))));

    assertEquals(
        List.of(
            RmiJavaDiagnosticCodes.UNSUPPORTED_TYPE_REFERENCE,
            RmiJavaDiagnosticCodes.UNSUPPORTED_TYPE_REFERENCE,
            RmiJavaDiagnosticCodes.UNSUPPORTED_IDL_TYPE_MAPPING,
            RmiJavaDiagnosticCodes.UNSUPPORTED_IDL_TYPE_MAPPING),
        diagnosticCodes(mapper.map(declaration)));
  }

  @Test
  void exposesImmutableMappingValues() {
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
    RmiJavaToIdlResult result = mapper.map(declaration);
    RmiIdlTranslationUnit translationUnit = result.translationUnit().orElseThrow();
    RmiIdlInterface idlInterface = translationUnit.modules().getFirst().interfaces().getFirst();

    assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
    assertThrows(UnsupportedOperationException.class, () -> translationUnit.modules().clear());
    assertThrows(UnsupportedOperationException.class, () -> idlInterface.operations().clear());
    assertThrows(
        IllegalArgumentException.class,
        () -> new RmiJavaToIdlResult(Optional.of(translationUnit), resultWithErrorCodes()));
  }

  @Test
  void keepsMappingDiagnosticCodeValuesStable() {
    assertEquals(
        List.of("RMI-0200", "RMI-0201", "RMI-0202", "RMI-0203", "RMI-0204", "RMI-0205"),
        List.of(
                RmiJavaDiagnosticCodes.UNSUPPORTED_IDL_TYPE_MAPPING,
                RmiJavaDiagnosticCodes.INVALID_IDL_MODULE_NAME,
                RmiJavaDiagnosticCodes.INVALID_IDL_INTERFACE_NAME,
                RmiJavaDiagnosticCodes.INVALID_IDL_OPERATION_NAME,
                RmiJavaDiagnosticCodes.INVALID_IDL_PARAMETER_NAME,
                RmiJavaDiagnosticCodes.INVALID_IDL_EXCEPTION_NAME)
            .stream()
            .map(DiagnosticCode::value)
            .toList());
  }

  private static List<DiagnosticCode> diagnosticCodes(RmiJavaToIdlResult result) {
    assertTrue(result.translationUnit().isEmpty());
    assertTrue(
        result.diagnostics().stream()
            .allMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR));
    return result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList();
  }

  private static List<io.github.mundanej.mjo.common.Diagnostic> resultWithErrorCodes() {
    return List.of(
        io.github.mundanej.mjo.common.Diagnostic.withoutSpan(
            RmiJavaDiagnosticCodes.INVALID_IDL_INTERFACE_NAME,
            DiagnosticSeverity.ERROR,
            "forced error"));
  }
}
