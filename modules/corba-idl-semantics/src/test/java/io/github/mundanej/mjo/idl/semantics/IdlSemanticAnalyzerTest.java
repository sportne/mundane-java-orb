package io.github.mundanej.mjo.idl.semantics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import io.github.mundanej.mjo.idl.parser.IdlParseResult;
import io.github.mundanej.mjo.idl.parser.IdlParser;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link IdlSemanticAnalyzer}. */
@Tag("unit")
final class IdlSemanticAnalyzerTest {

  private final IdlParser parser = new IdlParser();
  private final IdlSemanticAnalyzer analyzer = new IdlSemanticAnalyzer();

  @Test
  void analyzesMinimalSubsetIntoDeterministicSemanticModel() {
    String source =
        """
        module Demo {
          const long BASE = 1 + 2 * 3;
          const unsigned long MASK = (BASE << 2) | 1;
          const boolean ENABLED = TRUE;
          const char LETTER = 'A';
          const string NAME = "demo";
          const double RATIO = -1.5e2;
          enum Color { RED, GREEN };
          const Color FAVORITE = Color::GREEN;
          exception Bad { string reason; };
          interface Shape {
            attribute Later later;
            Later move(in Later value, out long count) raises (Bad);
          };
          struct Later { long x; };
        };
        """;

    IdlSemanticResult result = analyze(source);
    IdlSemanticResult repeated = analyze(source);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    assertEquals(result, repeated);
    IdlSemanticModel model = result.model().orElseThrow();
    assertEquals(
        List.of(
            "::Demo",
            "::Demo::BASE",
            "::Demo::MASK",
            "::Demo::ENABLED",
            "::Demo::LETTER",
            "::Demo::NAME",
            "::Demo::RATIO",
            "::Demo::Color",
            "::Demo::Color::RED",
            "::Demo::Color::GREEN",
            "::Demo::FAVORITE",
            "::Demo::Bad",
            "::Demo::Bad::reason",
            "::Demo::Shape",
            "::Demo::Shape::later",
            "::Demo::Shape::move",
            "::Demo::Shape::move::value",
            "::Demo::Shape::move::count",
            "::Demo::Later",
            "::Demo::Later::x"),
        model.symbols().stream().map(IdlSymbol::qualifiedName).toList());

    IdlSymbol mask = model.findSymbol("Demo::MASK").orElseThrow();
    assertEquals(IdlSymbolKind.CONSTANT, mask.kind());
    assertEquals("unsigned long", mask.typeName().orElseThrow());
    assertEquals("unsigned long", mask.resolvedTypeName().orElseThrow());
    IdlConstantValue.IntegerValue maskValue =
        assertInstanceOf(IdlConstantValue.IntegerValue.class, mask.constantValue().orElseThrow());
    assertEquals(BigInteger.valueOf(29), maskValue.value());

    IdlSymbol ratio = model.findSymbol("::Demo::RATIO").orElseThrow();
    IdlConstantValue.FloatingValue ratioValue =
        assertInstanceOf(IdlConstantValue.FloatingValue.class, ratio.constantValue().orElseThrow());
    assertEquals(0, new BigDecimal("-1.5e2").compareTo(ratioValue.value()));

    IdlSymbol favorite = model.findSymbol("::Demo::FAVORITE").orElseThrow();
    IdlConstantValue.EnumeratorValue favoriteValue =
        assertInstanceOf(
            IdlConstantValue.EnumeratorValue.class, favorite.constantValue().orElseThrow());
    assertEquals("::Demo::Color", favoriteValue.idlType());
    assertEquals("::Demo::Color::GREEN", favoriteValue.enumeratorName());

    assertEquals(
        "::Demo::Later",
        model.findSymbol("::Demo::Shape::move").orElseThrow().resolvedTypeName().orElseThrow());
    assertEquals(
        "::Demo::Later",
        model.findSymbol("::Demo::Shape::later").orElseThrow().resolvedTypeName().orElseThrow());
    assertEquals(1, model.findSymbol("::Demo").orElseThrow().span().start().line());
  }

  @Test
  void analyzesGeneratedRmiIdlFixtureSubset() {
    IdlSemanticResult result = analyze(generatedRmiFixture());

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    IdlSemanticModel model = result.model().orElseThrow();
    assertEquals(
        List.of(
            "::example",
            "::example::calc",
            "::example::calc::CalculatorProblem",
            "::example::calc::Calculator",
            "::example::calc::Calculator::add",
            "::example::calc::Calculator::add::left",
            "::example::calc::Calculator::add::right",
            "::example::calc::Calculator::describe",
            "::example::calc::Calculator::describe::name",
            "::example::calc::Calculator::clear"),
        model.symbols().stream().map(IdlSymbol::qualifiedName).toList());
    assertEquals(
        IdlSymbolKind.EXCEPTION,
        model.findSymbol("::example::calc::CalculatorProblem").orElseThrow().kind());
    assertEquals(
        "wstring",
        model
            .findSymbol("::example::calc::Calculator::describe")
            .orElseThrow()
            .resolvedTypeName()
            .orElseThrow());
    assertEquals(
        "long",
        model
            .findSymbol("::example::calc::Calculator::add::left")
            .orElseThrow()
            .resolvedTypeName()
            .orElseThrow());
  }

  @Test
  void semanticResultsAndModelsAreImmutableValues() {
    IdlSemanticResult result = analyze("module Demo { const long VALUE = 42; };");
    IdlSemanticModel model = result.model().orElseThrow();
    IdlSymbol symbol = model.findSymbol("Demo::VALUE").orElseThrow();

    assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
    assertThrows(UnsupportedOperationException.class, () -> model.symbols().clear());
    assertThrows(
        UnsupportedOperationException.class, () -> model.symbols(IdlSymbolKind.CONSTANT).clear());
    assertEquals(result, analyze("module Demo { const long VALUE = 42; };"));
    assertEquals(
        IdlConstantValue.integer("long", BigInteger.valueOf(42)), value(model, "Demo::VALUE"));
    assertThrows(IllegalArgumentException.class, () -> model.findSymbol(" "));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlSymbol(
                IdlSymbolKind.CONSTANT,
                "VALUE",
                "Demo::VALUE",
                Optional.of("long"),
                Optional.of("long"),
                Optional.empty(),
                symbol.span()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlSemanticResult(
                Optional.of(model),
                List.of(
                    Diagnostic.withoutSpan(
                        IdlSemanticDiagnosticCodes.UNRESOLVED_NAME,
                        DiagnosticSeverity.ERROR,
                        "forced error"))));
  }

  @Test
  void evaluatesAdditionalConstantOperatorsReferencesAndLiteralForms() {
    IdlSemanticModel model =
        analyze(
                """
                module Ops {
                  const long HEX = 0x10;
                  const long OCT = 010;
                  const long MIX = +HEX - -OCT + (~0 & 3) + (7 / 2) + (7 % 2) + (1 ^ 3) + (8 >> 1);
                  const double D1 = 0x10;
                  const double D2 = D1;
                  const boolean B1 = FALSE;
                  const boolean B2 = B1;
                  const char C1 = '\\n';
                  const char C2 = C1;
                  const string S1 = "a\\n";
                  const string S2 = S1;
                  enum E { _TRUE, OFF };
                  const E EV = ::Ops::E::_TRUE;
                  exception Problem {};
                  struct Holder { Problem problem; };
                  interface Peer {};
                  interface UsesPeer { Peer peer(); };
                };
                """)
            .model()
            .orElseThrow();

    assertEquals(BigInteger.valueOf(37), integerValue(model, "Ops::MIX").value());
    assertEquals(0, new BigDecimal("16").compareTo(floatingValue(model, "Ops::D1").value()));
    assertEquals(0, new BigDecimal("16").compareTo(floatingValue(model, "Ops::D2").value()));
    assertFalse(booleanValue(model, "Ops::B2").value());
    assertEquals("\n", characterValue(model, "Ops::C2").value());
    assertEquals("a\n", stringValue(model, "Ops::S2").value());
    assertEquals(IdlConstantValue.Kind.ENUMERATOR, value(model, "Ops::EV").kind());
    assertEquals(
        "::Ops::Problem",
        model.findSymbol("Ops::Holder::problem").orElseThrow().resolvedTypeName().orElseThrow());
    assertEquals(
        "::Ops::Peer",
        model.findSymbol("Ops::UsesPeer::peer").orElseThrow().resolvedTypeName().orElseThrow());
    assertEquals(
        List.of(IdlConstantValue.Kind.values()).size(), IdlConstantValue.Kind.values().length);
  }

  @Test
  void reportsDuplicateNamesInvalidConstantsAndInvalidRaisesTargets() {
    IdlSemanticResult result =
        analyze(
            """
            module Bad {
              struct Point { long x; short X; };
              enum Color { RED, red };
              const long FORWARD = LATER + 1;
              const long LATER = 2;
              struct point { long value; };
              const Point BAD_CONST = 1;
              exception Problem {};
              interface I {
                void op(in long value, out long Value) raises (Missing, Point);
              };
            };
            """);

    assertTrue(result.hasErrors());
    assertTrue(result.model().isEmpty());
    List<DiagnosticCode> codes = diagnosticCodes(result);
    assertEquals(4, count(codes, IdlSemanticDiagnosticCodes.DUPLICATE_NAME));
    assertEquals(1, count(codes, IdlSemanticDiagnosticCodes.INVALID_CONSTANT_EXPRESSION));
    assertEquals(1, count(codes, IdlSemanticDiagnosticCodes.INVALID_CONSTANT_VALUE));
    assertEquals(1, count(codes, IdlSemanticDiagnosticCodes.UNRESOLVED_NAME));
    assertEquals(1, count(codes, IdlSemanticDiagnosticCodes.INVALID_RAISES_TARGET));
  }

  @Test
  void reportsCaseMismatchedReferencesSeparatelyFromMissingNames() {
    IdlSemanticResult result =
        analyze(
            """
            module Demo {
              struct Point { long x; };
              interface I {
                point op();
              };
            };
            """);

    assertTrue(result.hasErrors());
    assertEquals(List.of(IdlSemanticDiagnosticCodes.CASE_MISMATCH), diagnosticCodes(result));
  }

  @Test
  void rejectsUnsupportedConstantExpressionShapes() {
    IdlSemanticResult result =
        analyze(
            """
            module Bad {
              const long DIVIDE = 1 / 0;
              const long SHIFT = 1 << -1;
              const long TRAILING = 1 +;
              const string JOINED = "a" "b";
              const double BAD_FLOAT = TRUE;
              const char TOO_LONG = 'ab';
              const Object BAD_OBJECT = 1;
              enum E { A };
              const E BAD_ENUM = E::MISSING;
            };
            """);

    assertTrue(result.hasErrors());
    List<DiagnosticCode> codes = diagnosticCodes(result);
    assertEquals(4, count(codes, IdlSemanticDiagnosticCodes.INVALID_CONSTANT_VALUE));
    assertEquals(4, count(codes, IdlSemanticDiagnosticCodes.INVALID_CONSTANT_EXPRESSION));
    assertEquals(2, count(codes, IdlSemanticDiagnosticCodes.UNRESOLVED_NAME));
  }

  private IdlSemanticResult analyze(String source) {
    IdlParseResult parseResult = parser.parse("semantic-test.idl", source);
    assertFalse(parseResult.hasErrors(), () -> parseResult.diagnostics().toString());
    return analyzer.analyze(parseResult.translationUnit().orElseThrow());
  }

  private static String generatedRmiFixture() {
    return """
        module example {
          module calc {
            exception CalculatorProblem {
            };
            interface Calculator {
              long add(in long left, in long right);
              wstring describe(in wstring name) raises (CalculatorProblem);
              void clear();
            };
          };
        };
        """;
  }

  private static IdlConstantValue value(IdlSemanticModel model, String qualifiedName) {
    return model.findSymbol(qualifiedName).orElseThrow().constantValue().orElseThrow();
  }

  private static IdlConstantValue.IntegerValue integerValue(
      IdlSemanticModel model, String qualifiedName) {
    return assertInstanceOf(IdlConstantValue.IntegerValue.class, value(model, qualifiedName));
  }

  private static IdlConstantValue.FloatingValue floatingValue(
      IdlSemanticModel model, String qualifiedName) {
    return assertInstanceOf(IdlConstantValue.FloatingValue.class, value(model, qualifiedName));
  }

  private static IdlConstantValue.BooleanValue booleanValue(
      IdlSemanticModel model, String qualifiedName) {
    return assertInstanceOf(IdlConstantValue.BooleanValue.class, value(model, qualifiedName));
  }

  private static IdlConstantValue.CharacterValue characterValue(
      IdlSemanticModel model, String qualifiedName) {
    return assertInstanceOf(IdlConstantValue.CharacterValue.class, value(model, qualifiedName));
  }

  private static IdlConstantValue.StringValue stringValue(
      IdlSemanticModel model, String qualifiedName) {
    return assertInstanceOf(IdlConstantValue.StringValue.class, value(model, qualifiedName));
  }

  private static List<DiagnosticCode> diagnosticCodes(IdlSemanticResult result) {
    return result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList();
  }

  private static long count(List<DiagnosticCode> codes, DiagnosticCode expected) {
    return codes.stream().filter(expected::equals).count();
  }
}
