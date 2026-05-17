package io.github.mundanej.mjo.idl.preprocessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.idl.lexer.IdlLexerOptions;
import io.github.mundanej.mjo.idl.lexer.IdlToken;
import io.github.mundanej.mjo.idl.lexer.IdlTokenKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link IdlPreprocessor}. */
@Tag("unit")
final class IdlPreprocessorTest {

  @TempDir private Path tempDir;

  @Test
  void expandsQuotedSystemAndNestedIncludesInEncounterOrder() throws IOException {
    Path nestedDir = Files.createDirectories(tempDir.resolve("nested"));
    Files.writeString(
        tempDir.resolve("common.idl"), "module Common {}\n#include <nested/Types.idl>\n");
    Files.writeString(nestedDir.resolve("Types.idl"), "interface Nested;\n");

    IdlPreprocessor preprocessor = new IdlPreprocessor(PathIdlIncludeResolver.of(tempDir));
    IdlPreprocessResult result =
        preprocessor.preprocess(
            new IdlSource("root.idl", "#include \"common.idl\"\ninterface Root;\n"));

    assertEquals(
        List.of("module", "Common", "{", "}", "interface", "Nested", ";", "interface", "Root", ";"),
        nonEofLexemes(result));
    assertEquals(
        List.of(
            tempDir.resolve("common.idl").toAbsolutePath().normalize().toString(),
            nestedDir.resolve("Types.idl").toAbsolutePath().normalize().toString()),
        result.includedSourceNames());
    assertFalse(result.hasErrors());
  }

  @Test
  void reportsMissingUnsafeCyclicAndDepthLimitedIncludes() throws IOException {
    IdlPreprocessResult missing =
        new IdlPreprocessor(PathIdlIncludeResolver.of(tempDir))
            .preprocess(new IdlSource("missing.idl", "#include \"missing.idl\"\n"));
    assertEquals(
        List.of(IdlPreprocessorDiagnosticCodes.INCLUDE_NOT_FOUND), diagnosticCodes(missing));

    IdlPreprocessResult unsafe =
        new IdlPreprocessor(PathIdlIncludeResolver.of(tempDir))
            .preprocess(new IdlSource("unsafe.idl", "#include \"../bad.idl\"\n"));
    assertEquals(
        List.of(IdlPreprocessorDiagnosticCodes.UNSAFE_INCLUDE_PATH), diagnosticCodes(unsafe));

    Path a = tempDir.resolve("a.idl");
    Path b = tempDir.resolve("b.idl");
    Files.writeString(a, "#include \"b.idl\"\n");
    Files.writeString(b, "#include \"a.idl\"\n");
    IdlPreprocessResult cycle =
        new IdlPreprocessor(PathIdlIncludeResolver.of(tempDir))
            .preprocess(
                new IdlSource(a.toAbsolutePath().normalize().toString(), Files.readString(a)));
    assertEquals(List.of(IdlPreprocessorDiagnosticCodes.INCLUDE_CYCLE), diagnosticCodes(cycle));

    IdlPreprocessorOptions depthZero =
        new IdlPreprocessorOptions(
            IdlLexerOptions.defaults(),
            new BoundedLimit("include-depth", 0),
            new BoundedLimit("macro-expansions", 10),
            new BoundedLimit("diagnostics", 10));
    IdlPreprocessResult depth =
        new IdlPreprocessor(PathIdlIncludeResolver.of(tempDir))
            .preprocess(new IdlSource("depth.idl", "#include \"a.idl\"\n"), depthZero);
    assertEquals(
        List.of(IdlPreprocessorDiagnosticCodes.INCLUDE_DEPTH_EXCEEDED), diagnosticCodes(depth));
  }

  @Test
  void normalizesLineContinuationsAndMapsTokenLocationsToOriginalSource() {
    IdlPreprocessResult result =
        new IdlPreprocessor()
            .preprocess(
                new IdlSource(
                    "continued.idl",
                    """
                    #define ALIAS unsigned \\
                    long
                    inter\\
                    face X;
                    ALIAS value;
                    """));

    List<IdlToken> tokens = nonEofTokens(result);
    IdlToken interfaceToken = tokens.getFirst();
    assertEquals("interface", interfaceToken.lexeme());
    assertEquals(3, interfaceToken.span().start().line());
    assertEquals(1, interfaceToken.span().start().column());
    assertEquals(4, interfaceToken.span().end().line());
    assertEquals(4, interfaceToken.span().end().column());

    IdlToken unsigned =
        tokens.stream()
            .filter(token -> token.lexeme().equals("unsigned"))
            .findFirst()
            .orElseThrow();
    IdlToken longToken =
        tokens.stream().filter(token -> token.lexeme().equals("long")).findFirst().orElseThrow();
    assertEquals(5, unsigned.span().start().line());
    assertEquals(1, unsigned.span().start().column());
    assertEquals(unsigned.span(), longToken.span());
    assertFalse(result.hasErrors());
  }

  @Test
  void expandsObjectAndSimpleFunctionLikeMacrosAndHonorsUndef() {
    IdlPreprocessResult result =
        new IdlPreprocessor()
            .preprocess(
                new IdlSource(
                    "macros.idl",
                    """
                    #define NAME Thing
                    #define FIELD(type, name) type name
                    module NAME { FIELD(long, count); }
                    #undef NAME
                    NAME after;
                    """));

    assertEquals(
        List.of("module", "Thing", "{", "long", "count", ";", "}", "NAME", "after", ";"),
        nonEofLexemes(result));
    IdlToken count =
        nonEofTokens(result).stream()
            .filter(token -> token.lexeme().equals("count"))
            .findFirst()
            .orElseThrow();
    assertEquals(3, count.span().start().line());
    assertTrue(count.span().start().column() > 20);
    assertFalse(result.hasErrors());
  }

  @Test
  void reportsMacroRedefinitionUnsupportedOperatorsRecursionAndExpansionLimits() {
    IdlPreprocessResult redefinition =
        new IdlPreprocessor()
            .preprocess(
                new IdlSource(
                    "macro-diagnostics.idl",
                    "#define A 1\n#define A 0\n#define S(x) # x\n#define P(x,y) x ## y\nA\n"));

    assertEquals(List.of("0"), nonEofLexemes(redefinition));
    assertEquals(
        List.of(
            IdlPreprocessorDiagnosticCodes.MACRO_REDEFINED,
            IdlPreprocessorDiagnosticCodes.UNSUPPORTED_MACRO_OPERATOR,
            IdlPreprocessorDiagnosticCodes.UNSUPPORTED_MACRO_OPERATOR),
        diagnosticCodes(redefinition));

    IdlPreprocessResult recursive =
        new IdlPreprocessor()
            .preprocess(new IdlSource("recursive.idl", "#define A B\n#define B A\nA\n"));
    assertEquals(
        List.of(IdlPreprocessorDiagnosticCodes.RECURSIVE_MACRO), diagnosticCodes(recursive));

    IdlPreprocessorOptions noExpansions =
        new IdlPreprocessorOptions(
            IdlLexerOptions.defaults(),
            new BoundedLimit("include-depth", 10),
            new BoundedLimit("macro-expansions", 0),
            new BoundedLimit("diagnostics", 10));
    IdlPreprocessResult limited =
        new IdlPreprocessor()
            .preprocess(new IdlSource("limited.idl", "#define A 1\nA\n"), noExpansions);
    assertEquals(
        List.of(IdlPreprocessorDiagnosticCodes.MACRO_EXPANSION_LIMIT_EXCEEDED),
        diagnosticCodes(limited));
  }

  @Test
  void handlesConditionalsAndReportsMalformedConditionalState() {
    IdlPreprocessResult result =
        new IdlPreprocessor()
            .preprocess(
                new IdlSource(
                    "conditionals.idl",
                    """
                    #define ON 1
                    #ifdef ON
                    interface Active;
                    #else
                    interface $;
                    #endif
                    #ifndef ON
                    interface No;
                    #elif defined(ON) && !defined(MISSING)
                    interface Elif;
                    #endif
                    #if (1 && !0)
                    interface IfTrue;
                    #endif
                    #if 2
                    interface BadIf;
                    #endif
                    #endif
                    """));

    assertEquals(
        List.of("interface", "Active", ";", "interface", "Elif", ";", "interface", "IfTrue", ";"),
        nonEofLexemes(result));
    assertEquals(
        List.of(
            IdlPreprocessorDiagnosticCodes.UNSUPPORTED_CONDITIONAL_EXPRESSION,
            IdlPreprocessorDiagnosticCodes.MALFORMED_CONDITIONAL),
        diagnosticCodes(result));
  }

  @Test
  void skipsInactiveConditionalExpressionDiagnostics() {
    IdlPreprocessResult result =
        new IdlPreprocessor()
            .preprocess(
                new IdlSource(
                    "skipped-conditionals.idl",
                    """
                    #if 1
                    interface Active;
                    #elif 2
                    interface SkippedElif;
                    #else
                    interface SkippedElse;
                    #endif
                    #if 0
                    #if 2
                    interface NestedSkipped;
                    #endif
                    #endif
                    """));

    assertEquals(List.of("interface", "Active", ";"), nonEofLexemes(result));
    assertEquals(List.of(), diagnosticCodes(result));
    assertFalse(result.hasErrors());
  }

  @Test
  void reportsUnterminatedConditionalsAndPassesThroughPragmaTokens() {
    IdlPreprocessResult result =
        new IdlPreprocessor()
            .preprocess(
                new IdlSource("pragma.idl", "#pragma prefix \"example\"\n#ifdef MISSING\n"));

    assertEquals(List.of("#", "pragma", "prefix", "\"example\""), nonEofLexemes(result));
    assertEquals(
        List.of(IdlPreprocessorDiagnosticCodes.UNTERMINATED_CONDITIONAL), diagnosticCodes(result));
  }

  @Test
  void mergesLexerAndPreprocessorDiagnosticsAndExposesImmutableLists() {
    IdlPreprocessResult result =
        new IdlPreprocessor()
            .preprocess(new IdlSource("diagnostics.idl", "interface $;\n#bogus\n"));

    assertEquals(
        List.of(
            new DiagnosticCode("IDL-0100"), IdlPreprocessorDiagnosticCodes.UNSUPPORTED_DIRECTIVE),
        diagnosticCodes(result));
    assertTrue(result.hasErrors());
    assertThrows(UnsupportedOperationException.class, () -> result.tokens().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.includedSourceNames().clear());
  }

  @Test
  void validatesPublicValues() {
    IdlPreprocessorOptions options = IdlPreprocessorOptions.defaults();
    assertEquals("idl-include-depth", options.includeDepthLimit().name());

    IdlSource source = new IdlSource("value.idl", "module M {}");
    IdlPreprocessResult result = new IdlPreprocessor().preprocess(source);
    assertEquals(source.sourceName(), result.tokens().getLast().span().start().sourceName());

    assertThrows(IllegalArgumentException.class, () -> new IdlSource(" ", ""));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new IdlIncludeRequest(
                "", IdlIncludeKind.QUOTED, "source.idl", result.tokens().getLast().span()));
  }

  private static List<IdlToken> nonEofTokens(IdlPreprocessResult result) {
    return result.tokens().stream()
        .filter(token -> token.kind() != IdlTokenKind.END_OF_FILE)
        .toList();
  }

  private static List<String> nonEofLexemes(IdlPreprocessResult result) {
    return nonEofTokens(result).stream().map(IdlToken::lexeme).toList();
  }

  private static List<DiagnosticCode> diagnosticCodes(IdlPreprocessResult result) {
    return result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList();
  }
}
