package io.github.mundanej.mjo.idl.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import io.github.mundanej.mjo.idl.ast.IdlAttribute;
import io.github.mundanej.mjo.idl.ast.IdlConstant;
import io.github.mundanej.mjo.idl.ast.IdlEnum;
import io.github.mundanej.mjo.idl.ast.IdlExceptionDeclaration;
import io.github.mundanej.mjo.idl.ast.IdlField;
import io.github.mundanej.mjo.idl.ast.IdlInterface;
import io.github.mundanej.mjo.idl.ast.IdlInterfaceForward;
import io.github.mundanej.mjo.idl.ast.IdlModule;
import io.github.mundanej.mjo.idl.ast.IdlOperation;
import io.github.mundanej.mjo.idl.ast.IdlParameterDirection;
import io.github.mundanej.mjo.idl.ast.IdlStruct;
import io.github.mundanej.mjo.idl.ast.IdlTranslationUnit;
import io.github.mundanej.mjo.idl.ast.IdlTypedef;
import io.github.mundanej.mjo.idl.ast.IdlUnion;
import io.github.mundanej.mjo.idl.lexer.IdlDiagnosticCodes;
import io.github.mundanej.mjo.idl.lexer.IdlToken;
import io.github.mundanej.mjo.idl.preprocessor.IdlPreprocessResult;
import io.github.mundanej.mjo.idl.preprocessor.IdlPreprocessor;
import io.github.mundanej.mjo.idl.preprocessor.IdlSource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link IdlParser}. */
@Tag("unit")
final class IdlParserTest {

  private final IdlParser parser = new IdlParser();

  @Test
  void parsesMinimalVerticalIdlSubsetIntoDeterministicAst() {
    String source =
        """
        module Demo {
          const unsigned long LIMIT = 1 + 2;
          struct Point { long x, y; };
          enum Color { RED, GREEN, BLUE };
          exception BadName { string reason; };
          interface Shape {
            readonly attribute string name;
            attribute long size, count;
            oneway void ping(in string message);
            Point move(in Point value, out long count) raises (Demo::BadName, ::External::Failure);
          };
        };
        """;

    IdlParseResult result = parser.parse("demo.idl", source);
    IdlParseResult repeated = parser.parse("demo.idl", source);

    assertFalse(result.hasErrors());
    assertEquals(result, repeated);
    IdlTranslationUnit unit = result.translationUnit().orElseThrow();
    IdlModule module = (IdlModule) unit.declarations().getFirst();
    assertEquals("Demo", module.name());
    assertEquals(5, module.declarations().size());
    assertEquals(1, module.span().start().line());
    assertEquals(1, module.span().start().column());

    IdlConstant constant = (IdlConstant) module.declarations().get(0);
    assertEquals("unsigned long", constant.type().name());
    assertEquals("LIMIT", constant.name());
    assertEquals(List.of("1", "+", "2"), constant.expression().lexemes());

    IdlStruct struct = (IdlStruct) module.declarations().get(1);
    assertEquals("Point", struct.name());
    assertEquals(List.of("x", "y"), struct.fields().stream().map(IdlField::name).toList());

    IdlEnum idlEnum = (IdlEnum) module.declarations().get(2);
    assertEquals(List.of("RED", "GREEN", "BLUE"), idlEnum.enumerators());

    IdlExceptionDeclaration exception = (IdlExceptionDeclaration) module.declarations().get(3);
    assertEquals("BadName", exception.name());
    assertEquals(List.of("reason"), exception.fields().stream().map(IdlField::name).toList());

    IdlInterface idlInterface = (IdlInterface) module.declarations().get(4);
    assertEquals("Shape", idlInterface.name());
    assertEquals(4, idlInterface.members().size());

    IdlAttribute readonly = (IdlAttribute) idlInterface.members().get(0);
    assertTrue(readonly.readonly());
    assertEquals("string", readonly.type().name());
    assertEquals(List.of("name"), readonly.names());

    IdlAttribute readWrite = (IdlAttribute) idlInterface.members().get(1);
    assertFalse(readWrite.readonly());
    assertEquals(List.of("size", "count"), readWrite.names());

    IdlOperation ping = (IdlOperation) idlInterface.members().get(2);
    assertTrue(ping.oneway());
    assertEquals("void", ping.returnType().name());
    assertEquals(IdlParameterDirection.IN, ping.parameters().getFirst().direction());

    IdlOperation move = (IdlOperation) idlInterface.members().get(3);
    assertEquals("Point", move.returnType().name());
    assertEquals(List.of("Demo::BadName", "::External::Failure"), move.raises());
  }

  @Test
  void parsesEmptyTranslationUnitWithDeterministicEofSpan() {
    IdlParseResult result = parser.parse("empty.idl", "");

    assertFalse(result.hasErrors());
    IdlTranslationUnit unit = result.translationUnit().orElseThrow();
    assertEquals(List.of(), unit.declarations());
    assertEquals("empty.idl", unit.span().start().sourceName());
    assertEquals(1, unit.span().start().line());
    assertEquals(1, unit.span().start().column());
    assertEquals(unit.span().start(), unit.span().end());
  }

  @Test
  void parsesGeneratedRmiIdlFixtureSubset() {
    IdlParseResult result = parser.parse("rmi-generated.idl", generatedRmiFixture());

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    IdlModule example =
        (IdlModule) result.translationUnit().orElseThrow().declarations().getFirst();
    IdlModule calc = (IdlModule) example.declarations().getFirst();
    IdlExceptionDeclaration problem = (IdlExceptionDeclaration) calc.declarations().get(0);
    IdlInterface calculator = (IdlInterface) calc.declarations().get(1);

    assertEquals("example", example.name());
    assertEquals("calc", calc.name());
    assertEquals("CalculatorProblem", problem.name());
    assertEquals(List.of(), problem.fields());
    assertEquals("Calculator", calculator.name());
    assertEquals(List.of("add", "describe", "clear"), operationNames(calculator));
    IdlOperation describe = (IdlOperation) calculator.members().get(1);
    assertEquals("wstring", describe.returnType().name());
    assertEquals(List.of("CalculatorProblem"), describe.raises());
  }

  @Test
  void parsesNestedModulesEscapedNamesAndPrimitiveTypes() {
    IdlParseResult result =
        parser.parse(
            "nested.idl",
            """
            module Root {
              module Child {
                struct _interface {
                  unsigned long long count;
                  long double ratio;
                  ::Root::Child::_interface self;
                };
              };
            };
            """);

    assertFalse(result.hasErrors());
    IdlModule root = (IdlModule) result.translationUnit().orElseThrow().declarations().getFirst();
    IdlModule child = (IdlModule) root.declarations().getFirst();
    IdlStruct struct = (IdlStruct) child.declarations().getFirst();

    assertEquals("interface", struct.name());
    assertEquals(
        List.of("unsigned long long", "long double", "::Root::Child::interface"),
        fieldTypes(struct));
  }

  @Test
  void parsesAllParameterDirectionsAndEmptyRaisesClauseAbsence() {
    IdlParseResult result =
        parser.parse(
            "directions.idl",
            """
            interface Ops {
              void mix(in long input, out string output, inout double both);
            };
            """);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    IdlInterface idlInterface =
        (IdlInterface) result.translationUnit().orElseThrow().declarations().getFirst();
    IdlOperation operation = (IdlOperation) idlInterface.members().getFirst();

    assertEquals("mix", operation.name());
    assertEquals(
        List.of(IdlParameterDirection.IN, IdlParameterDirection.OUT, IdlParameterDirection.INOUT),
        operation.parameters().stream().map(parameter -> parameter.direction()).toList());
    assertEquals(List.of(), operation.raises());
  }

  @Test
  void reportsUnsupportedDeclarationsTypesDeclaratorsAndPragmas() {
    IdlParseResult unsupportedDeclarations =
        parser.parse("unsupported.idl", "native Handle; component C; valuetype Value;");

    assertTrue(unsupportedDeclarations.hasErrors());
    assertEquals(
        List.of(
            IdlParserDiagnosticCodes.UNSUPPORTED_CONSTRUCT,
            IdlParserDiagnosticCodes.UNSUPPORTED_CONSTRUCT,
            IdlParserDiagnosticCodes.UNSUPPORTED_CONSTRUCT),
        diagnosticCodes(unsupportedDeclarations));
    assertTrue(unsupportedDeclarations.translationUnit().isEmpty());

    IdlParseResult unsupportedType = parser.parse("fixed.idl", "struct Bad { fixed value; };");
    assertEquals(
        List.of(IdlParserDiagnosticCodes.UNSUPPORTED_TYPE), diagnosticCodes(unsupportedType));

    IdlParseResult unsupportedDeclarator =
        parser.parse("bad-array.idl", "struct Bad { long values[]; };");
    assertEquals(
        List.of(IdlParserDiagnosticCodes.UNEXPECTED_TOKEN), diagnosticCodes(unsupportedDeclarator));

    IdlParseResult pragma = parser.parse("pragma.idl", "#pragma prefix \"example\"\n");
    assertEquals(List.of(IdlParserDiagnosticCodes.UNSUPPORTED_CONSTRUCT), diagnosticCodes(pragma));
  }

  @Test
  void parsesG10GrammarClosureConstructs() {
    IdlParseResult result =
        parser.parse(
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
              interface Base { void ping(); };
              interface Service : Base, Forward {
                attribute Count counts[LIMIT];
                void submit(in Names names, out Choice result);
              };
            };
            """);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    IdlModule module = (IdlModule) result.translationUnit().orElseThrow().declarations().getFirst();
    assertEquals("G10", module.name());
    assertTrue(module.declarations().get(1) instanceof IdlInterfaceForward);

    IdlTypedef names = (IdlTypedef) module.declarations().get(2);
    assertEquals("sequence<string<32>, LIMIT>", names.type().name());
    assertEquals("Names", names.declarators().getFirst().name());

    IdlTypedef matrix = (IdlTypedef) module.declarations().get(3);
    assertEquals(
        List.of("Matrix", "Count"), matrix.declarators().stream().map(d -> d.name()).toList());
    assertEquals(2, matrix.declarators().getFirst().dimensions().size());

    IdlUnion union = (IdlUnion) module.declarations().get(4);
    assertEquals("Choice", union.name());
    assertEquals(2, union.cases().size());
    assertEquals(2, union.cases().getFirst().labels().size());

    IdlInterface service = (IdlInterface) module.declarations().get(6);
    assertEquals(List.of("Base", "Forward"), service.baseInterfaces());
    IdlAttribute attribute = (IdlAttribute) service.members().getFirst();
    assertEquals(1, attribute.declarators().getFirst().dimensions().size());
  }

  @Test
  void parsesNestedSequencesWhenClosersShareShiftToken() {
    IdlParseResult result =
        parser.parse(
            "nested-sequence.idl",
            """
            module G10 {
              const unsigned long LIMIT = 4;
              typedef sequence<sequence<long, LIMIT>> Nested;
            };
            """);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    IdlModule module = (IdlModule) result.translationUnit().orElseThrow().declarations().getFirst();
    IdlTypedef nested = (IdlTypedef) module.declarations().get(1);
    assertEquals("sequence<sequence<long, LIMIT>>", nested.type().name());
  }

  @Test
  void reportsMalformedDeclarationsAndUnexpectedEndOfSource() {
    IdlParseResult missingSemicolon =
        parser.parse("missing.idl", "interface Broken { void op() } ;");
    assertEquals(
        List.of(IdlParserDiagnosticCodes.UNEXPECTED_TOKEN), diagnosticCodes(missingSemicolon));

    IdlParseResult unexpectedEnd =
        parser.parse("eof.idl", "module Broken { interface I { void op(in long value); };");
    assertTrue(diagnosticCodes(unexpectedEnd).contains(IdlParserDiagnosticCodes.UNEXPECTED_EOF));
    assertTrue(unexpectedEnd.translationUnit().isEmpty());
  }

  @Test
  void reportsDiagnosticSpanAtUnexpectedTokenAndRejectsErroredResultsWithAst() {
    IdlParseResult result = parser.parse("diagnostic-span.idl", "module Broken { interface ; };");

    assertTrue(result.hasErrors());
    assertEquals(List.of(IdlParserDiagnosticCodes.UNEXPECTED_TOKEN), diagnosticCodes(result));
    Diagnostic diagnostic = result.diagnostics().getFirst();
    assertEquals("diagnostic-span.idl", diagnostic.span().orElseThrow().start().sourceName());
    assertEquals(1, diagnostic.span().orElseThrow().start().line());
    assertEquals(27, diagnostic.span().orElseThrow().start().column());

    IdlTranslationUnit unit = parser.parse("empty.idl", "").translationUnit().orElseThrow();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlParseResult(
                Optional.of(unit),
                List.of(
                    Diagnostic.withoutSpan(
                        IdlParserDiagnosticCodes.UNEXPECTED_TOKEN,
                        DiagnosticSeverity.ERROR,
                        "forced"))));
  }

  @Test
  void flowsLexerAndPreprocessorDiagnosticsIntoParseResult() {
    IdlParseResult result = parser.parse("bad-lex.idl", "interface $;\n");

    assertEquals(List.of(IdlDiagnosticCodes.INVALID_CHARACTER), diagnosticCodes(result));
    assertTrue(result.hasErrors());
    assertTrue(result.translationUnit().isEmpty());
  }

  @Test
  void parseResultsAndPreprocessedTokenInputsAreImmutable() {
    IdlPreprocessResult preprocessResult =
        new IdlPreprocessor().preprocess(new IdlSource("immutable.idl", "module M {};"));
    IdlParseResult result = parser.parse(preprocessResult);

    assertFalse(result.hasErrors());
    assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
    assertThrows(
        UnsupportedOperationException.class,
        () -> result.translationUnit().orElseThrow().declarations().clear());

    List<IdlToken> withoutEof =
        preprocessResult.tokens().subList(0, preprocessResult.tokens().size() - 1);
    IdlPreprocessResult invalidTokens = new IdlPreprocessResult(withoutEof, List.of(), List.of());
    assertThrows(IllegalArgumentException.class, () -> parser.parse(invalidTokens));
  }

  @Test
  @Tag("security")
  void hostileParserInputsProduceBoundedDiagnostics() {
    String[] hostileInputs = {
      "module Broken { interface I { void op(in long value); };",
      "interface Bad { void op(in sequence<long value); };",
      "struct Bad { long values[]; };",
      "#if 2\ninterface Bad;\n#endif\n",
      "$ $ $ $"
    };

    for (String source : hostileInputs) {
      IdlParseResult result = parser.parse("hostile.idl", source);
      assertTrue(result.hasErrors(), source);
      assertTrue(result.diagnostics().size() <= 4, source);
    }
  }

  @Test
  @Tag("security")
  void boundedParserSmokeRemainsDeterministicAcrossRepeatedParses() {
    String source = "module Demo { interface Greeter { string greet(in string value); }; };";

    for (int iteration = 0; iteration < 128; iteration++) {
      IdlParseResult result = parser.parse("bounded.idl", source);
      assertFalse(result.hasErrors());
      IdlModule module =
          (IdlModule) result.translationUnit().orElseThrow().declarations().getFirst();
      IdlInterface idlInterface = (IdlInterface) module.declarations().getFirst();
      IdlOperation operation = (IdlOperation) idlInterface.members().getFirst();
      assertEquals("greet", operation.name());
      assertEquals("string", operation.returnType().name());
      assertEquals(IdlParameterDirection.IN, operation.parameters().getFirst().direction());
    }
  }

  private static List<String> fieldTypes(IdlStruct struct) {
    return struct.fields().stream().map(field -> field.type().name()).toList();
  }

  private static List<String> operationNames(IdlInterface idlInterface) {
    return idlInterface.members().stream()
        .map(IdlOperation.class::cast)
        .map(IdlOperation::name)
        .toList();
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

  private static List<DiagnosticCode> diagnosticCodes(IdlParseResult result) {
    return result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList();
  }
}
