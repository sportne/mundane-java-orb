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
import java.util.ArrayList;
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
  void analyzesG10GrammarClosureConstructs() {
    IdlSemanticResult result =
        analyze(
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
              interface Base { void ping(); };
              interface Service : Base, Forward {
                attribute Count counts[LIMIT];
                void submit(in Names names, out Choice result);
              };
            };
            """);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    IdlSemanticModel model = result.model().orElseThrow();
    assertEquals(
        List.of(
            "::G10::Forward",
            "::G10::Names",
            "::G10::Matrix",
            "::G10::Count",
            "::G10::Choice",
            "::G10::Choice::text",
            "::G10::Choice::names",
            "::G10::Base",
            "::G10::Service"),
        model.symbols().stream()
            .filter(
                symbol ->
                    symbol.kind() == IdlSymbolKind.INTERFACE
                        || symbol.kind() == IdlSymbolKind.TYPEDEF
                        || symbol.kind() == IdlSymbolKind.UNION
                        || symbol.qualifiedName().startsWith("::G10::Choice::"))
            .map(IdlSymbol::qualifiedName)
            .toList());
    assertEquals(
        "sequence<string<32>, LIMIT>",
        model.findSymbol("::G10::Names").orElseThrow().resolvedTypeName().orElseThrow());
    assertEquals(
        "::G10::Choice",
        model
            .findSymbol("::G10::Service::submit::result")
            .orElseThrow()
            .resolvedTypeName()
            .orElseThrow());
  }

  @Test
  void analyzesG12ValuetypeNativeAndRepositoryConstructs() {
    IdlSemanticResult result =
        analyze(
            """
            #pragma prefix "example.com"
            module G12 {
              native Handle;
              exception Problem {};
              abstract interface AbstractBase;
              local interface LocalControl { void ping() context ("tenant"); };
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

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    IdlSemanticModel model = result.model().orElseThrow();
    assertEquals(IdlSymbolKind.NATIVE, model.findSymbol("G12::Handle").orElseThrow().kind());
    assertEquals(IdlSymbolKind.VALUE_BOX, model.findSymbol("G12::NameValue").orElseThrow().kind());
    assertEquals(IdlSymbolKind.VALUETYPE, model.findSymbol("G12::Holder").orElseThrow().kind());
    assertEquals(
        "long", model.findSymbol("G12::Holder::id").orElseThrow().resolvedTypeName().orElseThrow());
    assertEquals(
        "sequence<string, 8>",
        model.findSymbol("G12::Holder::names").orElseThrow().resolvedTypeName().orElseThrow());
    assertEquals(
        "::G12::Handle",
        model
            .findSymbol("G12::Holder::touch::handle")
            .orElseThrow()
            .resolvedTypeName()
            .orElseThrow());
    assertEquals(
        IdlSymbolKind.VALUE_FACTORY, model.findSymbol("G12::Holder::create").orElseThrow().kind());
    assertEquals(
        "long",
        model.findSymbol("G12::Holder::create::id").orElseThrow().resolvedTypeName().orElseThrow());
    assertEquals(
        "IDL:example.com/G12/NameValue:1.0",
        model.findSymbol("G12::NameValue").orElseThrow().repositoryId().orElseThrow());
    assertEquals(
        "IDL:example.com/G12/Holder:1.0",
        model.findSymbol("G12::Holder").orElseThrow().repositoryId().orElseThrow());
  }

  @Test
  void enforcesG12ConstantRangesAndRepositoryPragmaEffects() {
    IdlSemanticModel model =
        analyze(
                """
                #pragma prefix "example.com"
                module Demo {
                  const uint8 BYTE = 0xff;
                  const int8 LOW = -128;
                  struct Thing { long id; };
                  #pragma version Thing 2.5
                  typeprefix Thing "example.com/custom";
                };
                """)
            .model()
            .orElseThrow();

    assertEquals(BigInteger.valueOf(255), integerValue(model, "Demo::BYTE").value());
    assertEquals(BigInteger.valueOf(-128), integerValue(model, "Demo::LOW").value());
    assertEquals(
        "IDL:example.com/custom/Thing:2.5",
        model.findSymbol("Demo::Thing").orElseThrow().repositoryId().orElseThrow());

    IdlSemanticModel predeclaredPragmaModel =
        analyze(
                """
                module Demo {
                  typeid Thing "IDL:predeclared/Thing:9.1";
                  struct Thing { long id; };
                };
                """)
            .model()
            .orElseThrow();
    assertEquals(
        "IDL:predeclared/Thing:9.1",
        predeclaredPragmaModel
            .findSymbol("Demo::Thing")
            .orElseThrow()
            .repositoryId()
            .orElseThrow());

    IdlSemanticResult rangeFailures =
        analyze(
            """
            module Bad {
              const uint8 TOO_LARGE = 256;
              const unsigned short NEGATIVE = -1;
            };
            """);
    assertEquals(
        2,
        count(diagnosticCodes(rangeFailures), IdlSemanticDiagnosticCodes.INVALID_CONSTANT_VALUE));
  }

  @Test
  void rejectsG12RecursiveTypesContextsPragmasAndAmbiguousNames() {
    IdlSemanticResult result =
        analyze(
            """
            module Bad {
              struct Direct { Direct next; };
              typedef Aliased AliasedRef;
              struct Aliased { AliasedRef next; };
              struct A { B b; };
              struct B { A a; };
              interface Left { void op(); };
              interface Right { void op(); };
              interface Both : Left, Right {};
              abstract interface Forward;
              local interface Forward {};
              interface Later {};
              local interface Later;
              abstract valuetype ValueForward;
              valuetype ValueForward {};
              valuetype V : W {};
              valuetype W : V {};
              interface Contexts {
                void duplicate() context ("tenant", "tenant");
                void wildcard() context ("*", "tenant");
              };
              struct Thing { long id; };
              typeid Thing "IDL:bad/Thing:1.0";
              typeid Thing "IDL:other/Thing:1.0";
            };
            """);

    assertTrue(result.hasErrors());
    List<DiagnosticCode> codes = diagnosticCodes(result);
    assertTrue(codes.contains(IdlSemanticDiagnosticCodes.INVALID_RECURSIVE_TYPE));
    assertTrue(codes.contains(IdlSemanticDiagnosticCodes.AMBIGUOUS_NAME));
    assertEquals(3, count(codes, IdlSemanticDiagnosticCodes.INVALID_FORWARD_DECLARATION));
    assertTrue(codes.contains(IdlSemanticDiagnosticCodes.INVALID_INHERITANCE));
    assertEquals(2, count(codes, IdlSemanticDiagnosticCodes.INVALID_OPERATION_CONTEXT));
    assertTrue(codes.contains(IdlSemanticDiagnosticCodes.INVALID_REPOSITORY_PRAGMA));
  }

  @Test
  void detectsInheritedAmbiguityAfterAllInterfaceBaseGraphsAreKnown() {
    IdlSemanticResult result =
        analyze(
            """
            module Bad {
              interface Left : Mid {};
              interface Both : Left, Right {};
              interface Mid : Top {};
              interface Top { void op(); };
              interface Right { void op(); };
            };
            """);

    assertEquals(List.of(IdlSemanticDiagnosticCodes.AMBIGUOUS_NAME), diagnosticCodes(result));
  }

  @Test
  void rejectsInvalidG10InheritanceBoundsAndUnionLabels() {
    IdlSemanticResult badBound =
        analyze("module Bad { typedef long Values[0]; interface I { void op(); }; };");
    assertTrue(
        diagnosticCodes(badBound).contains(IdlSemanticDiagnosticCodes.INVALID_CONSTANT_VALUE));

    IdlSemanticResult badInheritance =
        analyze("module Bad { struct S { long v; }; interface I : S { void op(); }; };");
    assertEquals(
        List.of(IdlSemanticDiagnosticCodes.INVALID_INHERITANCE), diagnosticCodes(badInheritance));

    IdlSemanticResult cyclicInheritance =
        analyze("module Bad { interface A : B {}; interface B : A {}; };");
    assertTrue(
        diagnosticCodes(cyclicInheritance)
            .contains(IdlSemanticDiagnosticCodes.INVALID_INHERITANCE));

    IdlSemanticResult duplicateUnionLabel =
        analyze(
            """
            module Bad {
              union Choice switch (long) {
                case 1: long a;
                case 1: long b;
              };
            };
            """);
    assertTrue(
        diagnosticCodes(duplicateUnionLabel)
            .contains(IdlSemanticDiagnosticCodes.INVALID_UNION_LABEL));

    IdlSemanticResult duplicateFactoryParameter =
        analyze("module Bad { valuetype V { factory create(in long id, in short ID); }; };");
    assertEquals(
        List.of(IdlSemanticDiagnosticCodes.DUPLICATE_NAME),
        diagnosticCodes(duplicateFactoryParameter));
  }

  @Test
  void validatesNestedSequenceTypesWithAdjacentClosers() {
    IdlSemanticResult result =
        analyze(
            """
            module G10 {
              const unsigned long LIMIT = 4;
              typedef sequence<sequence<long, LIMIT>> Nested;
              interface UsesNested {
                void op(in Nested values);
              };
            };
            """);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    assertEquals(
        "sequence<sequence<long, LIMIT>>",
        result
            .model()
            .orElseThrow()
            .findSymbol("::G10::Nested")
            .orElseThrow()
            .resolvedTypeName()
            .orElseThrow());
  }

  @Test
  void reportsInheritanceCyclesInDeclarationOrder() {
    IdlSemanticResult result =
        analyze(
            """
            module Bad {
              interface A : B {};
              interface B : A {};
            };
            """);

    assertEquals(
        List.of(
            "Interface inheritance cycle includes: ::Bad::A",
            "Interface inheritance cycle includes: ::Bad::B"),
        result.diagnostics().stream()
            .filter(
                diagnostic ->
                    diagnostic.code().equals(IdlSemanticDiagnosticCodes.INVALID_INHERITANCE))
            .map(Diagnostic::message)
            .toList());
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
  void resolvesRelativeNamesFromNearestScopeAndAbsoluteNamesFromGlobalScope() {
    IdlSemanticModel model =
        analyze(
                """
                module Root {
                  struct Value { long x; };
                  module Nested {
                    struct Value { short y; };
                    struct Holder {
                      Value nearby;
                      Root::Value global;
                    };
                  };
                };
                """)
            .model()
            .orElseThrow();

    assertEquals(
        "::Root::Nested::Value",
        model
            .findSymbol("Root::Nested::Holder::nearby")
            .orElseThrow()
            .resolvedTypeName()
            .orElseThrow());
    assertEquals(
        "::Root::Value",
        model
            .findSymbol("Root::Nested::Holder::global")
            .orElseThrow()
            .resolvedTypeName()
            .orElseThrow());
    assertEquals(
        List.of("::Root::Value", "::Root::Nested::Value", "::Root::Nested::Holder"),
        model.symbols(IdlSymbolKind.STRUCT).stream().map(IdlSymbol::qualifiedName).toList());
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

  @Test
  void reportsInvalidTypeReferencesWithStableDiagnosticSpan() {
    IdlSemanticResult result =
        analyze(
            """
            module Bad {
              const long VALUE = 1;
              struct UsesValueAsType { VALUE field; };
            };
            """);

    assertTrue(result.hasErrors());
    assertTrue(result.model().isEmpty());
    assertEquals(
        List.of(IdlSemanticDiagnosticCodes.INVALID_TYPE_REFERENCE), diagnosticCodes(result));
    Diagnostic diagnostic = result.diagnostics().getFirst();
    assertEquals("semantic-test.idl", diagnostic.span().orElseThrow().start().sourceName());
    assertEquals(3, diagnostic.span().orElseThrow().start().line());
  }

  @Test
  void validatesSemanticValueObjectsAndDefensiveCopiesSymbolLists() {
    IdlSemanticModel model =
        analyze("module Demo { const long VALUE = 42; };").model().orElseThrow();
    List<IdlSymbol> symbols = new ArrayList<>(model.symbols());
    IdlSemanticModel copied = new IdlSemanticModel(model.translationUnit(), symbols);

    symbols.clear();

    assertEquals(model.symbols(), copied.symbols());
    assertEquals(
        IdlConstantValue.Kind.INTEGER, IdlConstantValue.integer("long", BigInteger.ONE).kind());
    assertEquals(
        IdlConstantValue.Kind.FLOATING, IdlConstantValue.floating("double", BigDecimal.ONE).kind());
    assertEquals(IdlConstantValue.Kind.BOOLEAN, IdlConstantValue.bool("boolean", true).kind());
    assertEquals(IdlConstantValue.Kind.CHARACTER, IdlConstantValue.character("char", "x").kind());
    assertEquals(IdlConstantValue.Kind.STRING, IdlConstantValue.string("string", "").kind());
    assertEquals(
        IdlConstantValue.Kind.ENUMERATOR,
        IdlConstantValue.enumerator("::Demo::Color", "::Demo::Color::RED").kind());

    assertThrows(
        IllegalArgumentException.class, () -> IdlConstantValue.integer(" ", BigInteger.ONE));
    assertThrows(NullPointerException.class, () -> IdlConstantValue.integer("long", null));
    assertThrows(NullPointerException.class, () -> IdlConstantValue.floating("double", null));
    assertThrows(IllegalArgumentException.class, () -> IdlConstantValue.character("char", ""));
    assertThrows(NullPointerException.class, () -> IdlConstantValue.string("string", null));
    assertThrows(
        IllegalArgumentException.class, () -> IdlConstantValue.enumerator("::Demo::Color", " "));
    assertThrows(NullPointerException.class, () -> new IdlSemanticModel(null, List.of()));
    assertThrows(
        NullPointerException.class, () -> new IdlSemanticModel(model.translationUnit(), null));
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
