package io.github.mundanej.mjo.idl.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.common.DiagnosticCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link IdlLexer}. */
@Tag("unit")
final class IdlLexerTest {

  private final IdlLexer lexer = new IdlLexer();

  @Test
  void tokenizesIdentifiersKeywordsAndEscapedIdentifiers() {
    IdlLexResult result =
        lexer.tokenize("keywords.idl", "module M { interface _abstract { boolean Boolean; }; }");

    assertEquals(
        List.of(
            IdlTokenKind.KEYWORD,
            IdlTokenKind.IDENTIFIER,
            IdlTokenKind.LEFT_BRACE,
            IdlTokenKind.KEYWORD,
            IdlTokenKind.ESCAPED_IDENTIFIER,
            IdlTokenKind.LEFT_BRACE,
            IdlTokenKind.KEYWORD,
            IdlTokenKind.IDENTIFIER,
            IdlTokenKind.SEMICOLON,
            IdlTokenKind.RIGHT_BRACE,
            IdlTokenKind.SEMICOLON,
            IdlTokenKind.RIGHT_BRACE),
        nonEofKinds(result));
    assertEquals(Optional.of("M"), nonEofTokens(result).get(1).identifierText());
    assertEquals(Optional.of("abstract"), nonEofTokens(result).get(4).identifierText());
    assertEquals(List.of(IdlDiagnosticCodes.KEYWORD_CASE_COLLISION), diagnosticCodes(result));
    assertTrue(result.hasErrors());
  }

  @Test
  void tokenizesPunctuationOperatorsAndPreprocessorTokens() {
    IdlLexResult result =
        lexer.tokenize(
            "punctuation.idl",
            ":: << >> # ## ! || && ; { } : , = + - ( ) < > [ ] \\ | ^ & * / % ~ @");

    assertEquals(
        List.of(
            IdlTokenKind.DOUBLE_COLON,
            IdlTokenKind.SHIFT_LEFT,
            IdlTokenKind.SHIFT_RIGHT,
            IdlTokenKind.HASH,
            IdlTokenKind.DOUBLE_HASH,
            IdlTokenKind.EXCLAMATION,
            IdlTokenKind.LOGICAL_OR,
            IdlTokenKind.LOGICAL_AND,
            IdlTokenKind.SEMICOLON,
            IdlTokenKind.LEFT_BRACE,
            IdlTokenKind.RIGHT_BRACE,
            IdlTokenKind.COLON,
            IdlTokenKind.COMMA,
            IdlTokenKind.EQUALS,
            IdlTokenKind.PLUS,
            IdlTokenKind.MINUS,
            IdlTokenKind.LEFT_PAREN,
            IdlTokenKind.RIGHT_PAREN,
            IdlTokenKind.LESS_THAN,
            IdlTokenKind.GREATER_THAN,
            IdlTokenKind.LEFT_BRACKET,
            IdlTokenKind.RIGHT_BRACKET,
            IdlTokenKind.BACKSLASH,
            IdlTokenKind.VERTICAL_BAR,
            IdlTokenKind.CARET,
            IdlTokenKind.AMPERSAND,
            IdlTokenKind.ASTERISK,
            IdlTokenKind.SLASH,
            IdlTokenKind.PERCENT,
            IdlTokenKind.TILDE,
            IdlTokenKind.AT_SIGN),
        nonEofKinds(result));
    assertFalse(result.hasErrors());
  }

  @Test
  void tokenizesNumericLiteralsAndReportsMalformedNumbers() {
    IdlLexResult result =
        lexer.tokenize("numbers.idl", "12 014 0XC 1. .5 1e-2 1.2D 12d 09 0x 1e+ 0x1g");

    assertEquals(
        List.of(
            IdlTokenKind.INTEGER_LITERAL,
            IdlTokenKind.INTEGER_LITERAL,
            IdlTokenKind.INTEGER_LITERAL,
            IdlTokenKind.FLOATING_POINT_LITERAL,
            IdlTokenKind.FLOATING_POINT_LITERAL,
            IdlTokenKind.FLOATING_POINT_LITERAL,
            IdlTokenKind.FIXED_POINT_LITERAL,
            IdlTokenKind.FIXED_POINT_LITERAL,
            IdlTokenKind.INVALID_TOKEN,
            IdlTokenKind.INVALID_TOKEN,
            IdlTokenKind.INVALID_TOKEN,
            IdlTokenKind.INVALID_TOKEN),
        nonEofKinds(result));
    assertEquals(
        List.of(
            IdlDiagnosticCodes.INVALID_NUMERIC_LITERAL,
            IdlDiagnosticCodes.INVALID_NUMERIC_LITERAL,
            IdlDiagnosticCodes.INVALID_NUMERIC_LITERAL,
            IdlDiagnosticCodes.INVALID_NUMERIC_LITERAL),
        diagnosticCodes(result));
  }

  @Test
  void tokenizesCharacterAndStringLiterals() {
    IdlLexResult result =
        lexer.tokenize(
            "literals.idl", "'X' L'X' '\\0' L'\\u0000' \"Hello\\n\" L\"\\u3BC\" \"\\xA\" '\\141'");

    assertEquals(
        List.of(
            IdlTokenKind.CHARACTER_LITERAL,
            IdlTokenKind.WIDE_CHARACTER_LITERAL,
            IdlTokenKind.CHARACTER_LITERAL,
            IdlTokenKind.WIDE_CHARACTER_LITERAL,
            IdlTokenKind.STRING_LITERAL,
            IdlTokenKind.WIDE_STRING_LITERAL,
            IdlTokenKind.STRING_LITERAL,
            IdlTokenKind.CHARACTER_LITERAL),
        nonEofKinds(result));
    assertFalse(result.hasErrors());
  }

  @Test
  void reportsLiteralDiagnosticsAndRecovers() {
    IdlLexResult result =
        lexer.tokenize("bad-literals.idl", "\"\\q\" \"\\0\" L\"\\u0000\" \"unterminated\n'a");

    assertEquals(
        List.of(
            IdlTokenKind.STRING_LITERAL,
            IdlTokenKind.STRING_LITERAL,
            IdlTokenKind.WIDE_STRING_LITERAL,
            IdlTokenKind.INVALID_TOKEN,
            IdlTokenKind.INVALID_TOKEN),
        nonEofKinds(result));
    assertEquals(
        List.of(
            IdlDiagnosticCodes.INVALID_ESCAPE_SEQUENCE,
            IdlDiagnosticCodes.NUL_LITERAL_CHARACTER,
            IdlDiagnosticCodes.NUL_LITERAL_CHARACTER,
            IdlDiagnosticCodes.UNTERMINATED_STRING_LITERAL,
            IdlDiagnosticCodes.UNTERMINATED_CHARACTER_LITERAL),
        diagnosticCodes(result));
  }

  @Test
  void skipsCommentsAndTracksSourceLocations() {
    IdlLexResult result =
        lexer.tokenize(
            "comments.idl", "module X\r\n// comment\r\n/* block\n comment */interface Y");

    assertEquals(
        List.of(
            IdlTokenKind.KEYWORD,
            IdlTokenKind.IDENTIFIER,
            IdlTokenKind.KEYWORD,
            IdlTokenKind.IDENTIFIER),
        nonEofKinds(result));
    IdlToken interfaceToken = nonEofTokens(result).get(2);
    assertEquals("interface", interfaceToken.lexeme());
    assertEquals(4, interfaceToken.span().start().line());
    assertEquals(12, interfaceToken.span().start().column());
    assertFalse(result.hasErrors());
  }

  @Test
  void tracksCarriageReturnLineEndings() {
    IdlLexResult result = lexer.tokenize("cr.idl", "module\rinterface");

    IdlToken interfaceToken = nonEofTokens(result).get(1);
    assertEquals("interface", interfaceToken.lexeme());
    assertEquals(2, interfaceToken.span().start().line());
    assertEquals(1, interfaceToken.span().start().column());
  }

  @Test
  void reportsUnterminatedBlockComment() {
    IdlLexResult result = lexer.tokenize("comment.idl", "module X /* nope");

    assertEquals(List.of(IdlTokenKind.KEYWORD, IdlTokenKind.IDENTIFIER), nonEofKinds(result));
    assertEquals(List.of(IdlDiagnosticCodes.UNTERMINATED_BLOCK_COMMENT), diagnosticCodes(result));
  }

  @Test
  void exposesEndOfFileSpanForEmptySource() {
    IdlLexResult result = lexer.tokenize("empty.idl", "");

    assertEquals(
        List.of(IdlTokenKind.END_OF_FILE), result.tokens().stream().map(IdlToken::kind).toList());
    IdlToken eof = result.tokens().getFirst();
    assertEquals(1, eof.span().start().line());
    assertEquals(1, eof.span().start().column());
    assertEquals(0, eof.span().start().offset());
    assertEquals(eof.span().start(), eof.span().end());
  }

  @Test
  void resultListsAreImmutable() {
    IdlLexResult result = lexer.tokenize("immutable.idl", "module M");

    assertThrows(UnsupportedOperationException.class, () -> result.tokens().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
  }

  @Test
  void enforcesSourceTokenAndDiagnosticLimits() {
    IdlLexerOptions sourceLimitOptions =
        new IdlLexerOptions(
            new BoundedLimit("source", 3),
            new BoundedLimit("tokens", 10),
            new BoundedLimit("token-length", 10),
            new BoundedLimit("diagnostics", 10));
    IdlLexResult sourceLimit = lexer.tokenize("source-limit.idl", "module", sourceLimitOptions);
    assertEquals(List.of(IdlDiagnosticCodes.SOURCE_LIMIT_EXCEEDED), diagnosticCodes(sourceLimit));
    assertEquals(
        List.of(IdlTokenKind.END_OF_FILE),
        sourceLimit.tokens().stream().map(IdlToken::kind).toList());

    IdlLexerOptions tokenLimitOptions =
        new IdlLexerOptions(
            new BoundedLimit("source", 100),
            new BoundedLimit("tokens", 1),
            new BoundedLimit("token-length", 10),
            new BoundedLimit("diagnostics", 10));
    IdlLexResult tokenLimit = lexer.tokenize("token-limit.idl", "module X", tokenLimitOptions);
    assertEquals(List.of(IdlTokenKind.KEYWORD), nonEofKinds(tokenLimit));
    assertEquals(List.of(IdlDiagnosticCodes.TOKEN_LIMIT_EXCEEDED), diagnosticCodes(tokenLimit));

    IdlLexerOptions tokenLengthOptions =
        new IdlLexerOptions(
            new BoundedLimit("source", 100),
            new BoundedLimit("tokens", 10),
            new BoundedLimit("token-length", 3),
            new BoundedLimit("diagnostics", 10));
    IdlLexResult tokenLength = lexer.tokenize("token-length.idl", "module", tokenLengthOptions);
    assertEquals(List.of(IdlTokenKind.INVALID_TOKEN), nonEofKinds(tokenLength));
    assertEquals(
        List.of(IdlDiagnosticCodes.TOKEN_LENGTH_LIMIT_EXCEEDED), diagnosticCodes(tokenLength));

    IdlLexerOptions diagnosticLimitOptions =
        new IdlLexerOptions(
            new BoundedLimit("source", 100),
            new BoundedLimit("tokens", 10),
            new BoundedLimit("token-length", 10),
            new BoundedLimit("diagnostics", 2));
    IdlLexResult diagnosticLimit =
        lexer.tokenize("diagnostic-limit.idl", "$ $ $", diagnosticLimitOptions);
    assertEquals(
        List.of(IdlDiagnosticCodes.INVALID_CHARACTER, IdlDiagnosticCodes.DIAGNOSTIC_LIMIT_EXCEEDED),
        diagnosticCodes(diagnosticLimit));
  }

  @Test
  void validatesPublicValueObjects() {
    IdlToken identifier = nonEofTokens(lexer.tokenize("identifier.idl", "_thing")).getFirst();
    assertEquals(Optional.of("thing"), identifier.identifierText());

    assertThrows(
        IllegalArgumentException.class,
        () -> new IdlToken(IdlTokenKind.END_OF_FILE, "not-empty", identifier.span()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new IdlToken(IdlTokenKind.IDENTIFIER, "", identifier.span()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new IdlToken(IdlTokenKind.ESCAPED_IDENTIFIER, "thing", identifier.span()));
    assertThrows(
        NullPointerException.class,
        () ->
            new IdlLexerOptions(
                null,
                new BoundedLimit("tokens", 1),
                new BoundedLimit("token-length", 1),
                new BoundedLimit("diagnostics", 1)));
  }

  private static List<IdlToken> nonEofTokens(IdlLexResult result) {
    return result.tokens().stream()
        .filter(token -> token.kind() != IdlTokenKind.END_OF_FILE)
        .toList();
  }

  private static List<IdlTokenKind> nonEofKinds(IdlLexResult result) {
    return nonEofTokens(result).stream().map(IdlToken::kind).toList();
  }

  private static List<DiagnosticCode> diagnosticCodes(IdlLexResult result) {
    return result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList();
  }
}
