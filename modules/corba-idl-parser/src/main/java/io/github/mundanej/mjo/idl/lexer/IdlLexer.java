package io.github.mundanej.mjo.idl.lexer;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import io.github.mundanej.mjo.common.LimitViolation;
import io.github.mundanej.mjo.common.SourcePosition;
import io.github.mundanej.mjo.common.SourceSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Lexer for OMG IDL 4.2 lexical tokens. */
public final class IdlLexer {

  private static final Set<String> KEYWORDS =
      Set.of(
          "abstract",
          "any",
          "alias",
          "attribute",
          "bitfield",
          "bitmask",
          "bitset",
          "boolean",
          "case",
          "char",
          "component",
          "connector",
          "const",
          "consumes",
          "context",
          "custom",
          "default",
          "double",
          "exception",
          "emits",
          "enum",
          "eventtype",
          "factory",
          "FALSE",
          "finder",
          "fixed",
          "float",
          "getraises",
          "home",
          "import",
          "in",
          "inout",
          "interface",
          "local",
          "long",
          "manages",
          "map",
          "mirrorport",
          "module",
          "multiple",
          "native",
          "Object",
          "octet",
          "oneway",
          "out",
          "primarykey",
          "private",
          "port",
          "porttype",
          "provides",
          "public",
          "publishes",
          "raises",
          "readonly",
          "setraises",
          "sequence",
          "short",
          "string",
          "struct",
          "supports",
          "switch",
          "TRUE",
          "truncatable",
          "typedef",
          "typeid",
          "typename",
          "typeprefix",
          "unsigned",
          "union",
          "uses",
          "ValueBase",
          "valuetype",
          "void",
          "wchar",
          "wstring",
          "int8",
          "uint8",
          "int16",
          "int32",
          "int64",
          "uint16",
          "uint32",
          "uint64");

  /** Creates a stateless IDL lexer. */
  public IdlLexer() {}

  /** Lexes source using default bounded scanning options. */
  public IdlLexResult tokenize(String sourceName, String source) {
    return tokenize(sourceName, source, IdlLexerOptions.defaults());
  }

  /** Lexes source using caller-supplied bounded scanning options. */
  public IdlLexResult tokenize(String sourceName, String source, IdlLexerOptions options) {
    return new Scanner(sourceName, source, options).scan();
  }

  private static boolean isKeyword(String value) {
    return KEYWORDS.contains(value);
  }

  private static boolean collidesWithKeyword(String value) {
    return KEYWORDS.stream().anyMatch(keyword -> keyword.equalsIgnoreCase(value));
  }

  private static final class Scanner {

    private final String sourceName;
    private final String source;
    private final IdlLexerOptions options;
    private final List<IdlToken> tokens = new ArrayList<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    private int index;
    private int line = 1;
    private int column = 1;
    private SourcePosition lastConsumedPosition;
    private boolean stopped;
    private boolean diagnosticLimitReached;

    private Scanner(String sourceName, String source, IdlLexerOptions options) {
      this.sourceName = requireNonBlank(sourceName, "sourceName");
      this.source = Objects.requireNonNull(source, "source");
      this.options = Objects.requireNonNull(options, "options");
      this.lastConsumedPosition = currentPosition();
    }

    private IdlLexResult scan() {
      options
          .sourceLengthLimit()
          .check(source.length())
          .ifPresent(
              violation ->
                  emitDiagnostic(
                      IdlDiagnosticCodes.SOURCE_LIMIT_EXCEEDED,
                      violation.message(),
                      currentPosition(),
                      currentPosition()));
      if (!diagnostics.isEmpty()) {
        addEofToken();
        return new IdlLexResult(tokens, diagnostics);
      }

      while (!isAtEnd() && !stopped) {
        skipWhitespaceAndComments();
        if (isAtEnd() || stopped) {
          break;
        }
        scanToken();
      }
      addEofToken();
      return new IdlLexResult(tokens, diagnostics);
    }

    private void scanToken() {
      char current = peek();
      if (current == 'L' && (peekNext() == '\'' || peekNext() == '"')) {
        scanQuotedLiteral(true, peekNext());
      } else if (isAsciiLetter(current)) {
        scanIdentifier();
      } else if (current == '_') {
        scanEscapedIdentifier();
      } else if (isDecimalDigit(current)) {
        scanNumber();
      } else if (current == '.' && isDecimalDigit(peekNext())) {
        scanNumberStartingWithDot();
      } else if (current == '\'' || current == '"') {
        scanQuotedLiteral(false, current);
      } else {
        scanPunctuationOrInvalid();
      }
    }

    private void scanIdentifier() {
      SourcePosition start = currentPosition();
      advance();
      consumeIdentifierTail();
      String lexeme = lexemeFrom(start);
      if (isKeyword(lexeme)) {
        addToken(IdlTokenKind.KEYWORD, start);
      } else {
        if (collidesWithKeyword(lexeme)) {
          emitDiagnostic(
              IdlDiagnosticCodes.KEYWORD_CASE_COLLISION,
              "Identifier collides with IDL keyword using different case: " + lexeme,
              start,
              lastConsumedPosition);
        }
        addToken(IdlTokenKind.IDENTIFIER, start);
      }
    }

    private void scanEscapedIdentifier() {
      SourcePosition start = currentPosition();
      advance();
      if (!isAsciiLetter(peek())) {
        consumeIdentifierTail();
        emitDiagnostic(
            IdlDiagnosticCodes.INVALID_CHARACTER,
            "Escaped IDL identifier must be followed by an ASCII letter",
            start,
            lastConsumedPosition);
        addToken(IdlTokenKind.INVALID_TOKEN, start);
        return;
      }
      advance();
      consumeIdentifierTail();
      addToken(IdlTokenKind.ESCAPED_IDENTIFIER, start);
    }

    private void scanNumber() {
      SourcePosition start = currentPosition();
      if (peek() == '0' && (peekNext() == 'x' || peekNext() == 'X')) {
        scanHexInteger(start);
        return;
      }

      int integerStart = index;
      consumeDecimalDigits();
      boolean sawDot = false;
      boolean sawExponent = false;
      boolean invalid = false;

      if (peek() == '.') {
        sawDot = true;
        advance();
        consumeDecimalDigits();
      }
      if (!sawExponent && (peek() == 'd' || peek() == 'D')) {
        advance();
        invalid = consumeInvalidNumericTail();
        finishNumber(start, invalid, IdlTokenKind.FIXED_POINT_LITERAL);
        return;
      }
      if (peek() == 'e' || peek() == 'E') {
        sawExponent = true;
        advance();
        if (peek() == '+' || peek() == '-') {
          advance();
        }
        if (!isDecimalDigit(peek())) {
          invalid = true;
        } else {
          consumeDecimalDigits();
        }
      }
      if (sawDot || sawExponent) {
        invalid = consumeInvalidNumericTail() || invalid;
        finishNumber(start, invalid, IdlTokenKind.FLOATING_POINT_LITERAL);
        return;
      }

      invalid = containsInvalidOctalDigit(integerStart, index);
      invalid = consumeInvalidNumericTail() || invalid;
      finishNumber(start, invalid, IdlTokenKind.INTEGER_LITERAL);
    }

    private void scanNumberStartingWithDot() {
      SourcePosition start = currentPosition();
      advance();
      consumeDecimalDigits();
      boolean invalid = false;
      IdlTokenKind kind = IdlTokenKind.FLOATING_POINT_LITERAL;
      if (peek() == 'd' || peek() == 'D') {
        advance();
        kind = IdlTokenKind.FIXED_POINT_LITERAL;
      } else if (peek() == 'e' || peek() == 'E') {
        advance();
        if (peek() == '+' || peek() == '-') {
          advance();
        }
        if (!isDecimalDigit(peek())) {
          invalid = true;
        } else {
          consumeDecimalDigits();
        }
      }
      invalid = consumeInvalidNumericTail() || invalid;
      finishNumber(start, invalid, kind);
    }

    private void scanHexInteger(SourcePosition start) {
      advance();
      advance();
      int digitCount = 0;
      while (isHexDigit(peek())) {
        advance();
        digitCount++;
      }
      boolean invalid = digitCount == 0;
      invalid = consumeInvalidNumericTail() || invalid;
      finishNumber(start, invalid, IdlTokenKind.INTEGER_LITERAL);
    }

    private void finishNumber(SourcePosition start, boolean invalid, IdlTokenKind validKind) {
      if (invalid) {
        emitDiagnostic(
            IdlDiagnosticCodes.INVALID_NUMERIC_LITERAL,
            "Malformed IDL numeric literal: " + lexemeFrom(start),
            start,
            lastConsumedPosition);
        addToken(IdlTokenKind.INVALID_TOKEN, start);
      } else {
        addToken(validKind, start);
      }
    }

    private void scanQuotedLiteral(boolean wide, char quote) {
      SourcePosition start = currentPosition();
      if (wide) {
        advance();
      }
      advance();
      boolean terminated = false;
      while (!isAtEnd() && !stopped) {
        char current = peek();
        if (current == quote) {
          advance();
          terminated = true;
          break;
        }
        if (current == '\r' || current == '\n') {
          break;
        }
        if (current == '\0') {
          SourcePosition nulStart = currentPosition();
          advance();
          emitNulIfStringLiteral(quote, nulStart);
        } else if (current == '\\') {
          scanEscape(wide, quote);
        } else {
          if (current > 0x00FF) {
            SourcePosition invalidStart = currentPosition();
            advance();
            emitDiagnostic(
                IdlDiagnosticCodes.INVALID_CHARACTER,
                "IDL string and character literals are limited to ISO Latin-1 characters",
                invalidStart,
                lastConsumedPosition);
          } else {
            advance();
          }
        }
      }

      if (!terminated) {
        DiagnosticCode code =
            quote == '"'
                ? IdlDiagnosticCodes.UNTERMINATED_STRING_LITERAL
                : IdlDiagnosticCodes.UNTERMINATED_CHARACTER_LITERAL;
        emitDiagnostic(code, "Unterminated IDL literal", start, currentDiagnosticEnd(start));
        addToken(IdlTokenKind.INVALID_TOKEN, start);
        return;
      }

      addToken(literalKind(wide, quote), start);
    }

    private void scanEscape(boolean wide, char quote) {
      SourcePosition start = currentPosition();
      advance();
      if (isAtEnd()) {
        emitInvalidEscape(start);
        return;
      }

      char escaped = peek();
      if (isSimpleEscape(escaped)) {
        advance();
      } else if (isOctalDigit(escaped)) {
        int value = 0;
        int digits = 0;
        while (digits < 3 && isOctalDigit(peek())) {
          value = (value * 8) + (peek() - '0');
          advance();
          digits++;
        }
        emitNulEscapeIfNeeded(quote, value, start);
      } else if (escaped == 'x') {
        scanHexEscape(quote, start);
      } else if (escaped == 'u') {
        scanUnicodeEscape(wide, quote, start);
      } else {
        advance();
        emitInvalidEscape(start);
      }
    }

    private void scanHexEscape(char quote, SourcePosition start) {
      advance();
      int value = 0;
      int digits = 0;
      while (digits < 2 && isHexDigit(peek())) {
        value = (value * 16) + Character.digit(peek(), 16);
        advance();
        digits++;
      }
      if (digits == 0) {
        emitInvalidEscape(start);
      } else {
        emitNulEscapeIfNeeded(quote, value, start);
      }
    }

    private void scanUnicodeEscape(boolean wide, char quote, SourcePosition start) {
      advance();
      int value = 0;
      int digits = 0;
      while (digits < 4 && isHexDigit(peek())) {
        value = (value * 16) + Character.digit(peek(), 16);
        advance();
        digits++;
      }
      if (!wide || digits == 0) {
        emitInvalidEscape(start);
      } else {
        emitNulEscapeIfNeeded(quote, value, start);
      }
    }

    private void scanPunctuationOrInvalid() {
      SourcePosition start = currentPosition();
      if (startsWith("::")) {
        advance();
        advance();
        addToken(IdlTokenKind.DOUBLE_COLON, start);
      } else if (startsWith("<<")) {
        advance();
        advance();
        addToken(IdlTokenKind.SHIFT_LEFT, start);
      } else if (startsWith(">>")) {
        advance();
        advance();
        addToken(IdlTokenKind.SHIFT_RIGHT, start);
      } else if (startsWith("##")) {
        advance();
        advance();
        addToken(IdlTokenKind.DOUBLE_HASH, start);
      } else if (startsWith("||")) {
        advance();
        advance();
        addToken(IdlTokenKind.LOGICAL_OR, start);
      } else if (startsWith("&&")) {
        advance();
        advance();
        addToken(IdlTokenKind.LOGICAL_AND, start);
      } else {
        scanSingleCharacterToken(start);
      }
    }

    private void scanSingleCharacterToken(SourcePosition start) {
      char current = advance();
      IdlTokenKind kind =
          switch (current) {
            case ';' -> IdlTokenKind.SEMICOLON;
            case '{' -> IdlTokenKind.LEFT_BRACE;
            case '}' -> IdlTokenKind.RIGHT_BRACE;
            case ':' -> IdlTokenKind.COLON;
            case ',' -> IdlTokenKind.COMMA;
            case '=' -> IdlTokenKind.EQUALS;
            case '+' -> IdlTokenKind.PLUS;
            case '-' -> IdlTokenKind.MINUS;
            case '(' -> IdlTokenKind.LEFT_PAREN;
            case ')' -> IdlTokenKind.RIGHT_PAREN;
            case '<' -> IdlTokenKind.LESS_THAN;
            case '>' -> IdlTokenKind.GREATER_THAN;
            case '[' -> IdlTokenKind.LEFT_BRACKET;
            case ']' -> IdlTokenKind.RIGHT_BRACKET;
            case '\\' -> IdlTokenKind.BACKSLASH;
            case '|' -> IdlTokenKind.VERTICAL_BAR;
            case '^' -> IdlTokenKind.CARET;
            case '&' -> IdlTokenKind.AMPERSAND;
            case '*' -> IdlTokenKind.ASTERISK;
            case '/' -> IdlTokenKind.SLASH;
            case '%' -> IdlTokenKind.PERCENT;
            case '~' -> IdlTokenKind.TILDE;
            case '@' -> IdlTokenKind.AT_SIGN;
            case '#' -> IdlTokenKind.HASH;
            case '!' -> IdlTokenKind.EXCLAMATION;
            default -> IdlTokenKind.INVALID_TOKEN;
          };
      if (kind == IdlTokenKind.INVALID_TOKEN) {
        emitDiagnostic(
            IdlDiagnosticCodes.INVALID_CHARACTER,
            "Invalid IDL character: " + current,
            start,
            lastConsumedPosition);
      }
      addToken(kind, start);
    }

    private void skipWhitespaceAndComments() {
      boolean skipped;
      do {
        skipped = false;
        while (isWhitespace(peek())) {
          advance();
          skipped = true;
        }
        if (startsWith("//")) {
          advance();
          advance();
          while (!isAtEnd() && peek() != '\r' && peek() != '\n') {
            advance();
          }
          skipped = true;
        } else if (startsWith("/*")) {
          skipBlockComment();
          skipped = true;
        }
      } while (skipped && !stopped);
    }

    private void skipBlockComment() {
      SourcePosition start = currentPosition();
      advance();
      advance();
      while (!isAtEnd()) {
        if (startsWith("*/")) {
          advance();
          advance();
          return;
        }
        advance();
      }
      emitDiagnostic(
          IdlDiagnosticCodes.UNTERMINATED_BLOCK_COMMENT,
          "Unterminated IDL block comment",
          start,
          currentDiagnosticEnd(start));
    }

    private void addToken(IdlTokenKind kind, SourcePosition start) {
      int length = index - Math.toIntExact(start.offset());
      IdlTokenKind emittedKind = kind;
      options
          .tokenLengthLimit()
          .check(length)
          .ifPresent(
              violation -> {
                emitDiagnostic(
                    IdlDiagnosticCodes.TOKEN_LENGTH_LIMIT_EXCEEDED,
                    violation.message(),
                    start,
                    currentDiagnosticEnd(start));
              });
      if (stopped) {
        return;
      }
      if (length > options.tokenLengthLimit().maximum()) {
        emittedKind = IdlTokenKind.INVALID_TOKEN;
      }

      long nextTokenCount = tokens.size() + 1L;
      options
          .tokenCountLimit()
          .check(nextTokenCount)
          .ifPresent(
              violation -> {
                emitDiagnostic(
                    IdlDiagnosticCodes.TOKEN_LIMIT_EXCEEDED,
                    violation.message(),
                    start,
                    currentDiagnosticEnd(start));
                stopped = true;
              });
      if (!stopped) {
        tokens.add(new IdlToken(emittedKind, lexemeFrom(start), span(start, lastConsumedPosition)));
      }
    }

    private void addEofToken() {
      SourcePosition eof = currentPosition();
      tokens.add(new IdlToken(IdlTokenKind.END_OF_FILE, "", span(eof, eof)));
    }

    private void emitDiagnostic(
        DiagnosticCode code, String message, SourcePosition start, SourcePosition end) {
      if (diagnosticLimitReached) {
        return;
      }
      long maximum = options.diagnosticCountLimit().maximum();
      if (maximum == 0 || diagnostics.size() >= maximum) {
        diagnosticLimitReached = true;
        stopped = true;
        return;
      }
      if (!IdlDiagnosticCodes.DIAGNOSTIC_LIMIT_EXCEEDED.equals(code)
          && diagnostics.size() == maximum - 1) {
        diagnostics.add(
            Diagnostic.withSpan(
                IdlDiagnosticCodes.DIAGNOSTIC_LIMIT_EXCEEDED,
                DiagnosticSeverity.ERROR,
                new LimitViolation(options.diagnosticCountLimit(), diagnostics.size() + 1L)
                    .message(),
                span(start, end)));
        diagnosticLimitReached = true;
        stopped = true;
        return;
      }
      diagnostics.add(
          Diagnostic.withSpan(code, DiagnosticSeverity.ERROR, message, span(start, end)));
    }

    private void emitInvalidEscape(SourcePosition start) {
      emitDiagnostic(
          IdlDiagnosticCodes.INVALID_ESCAPE_SEQUENCE,
          "Invalid IDL escape sequence: " + lexemeFrom(start),
          start,
          currentDiagnosticEnd(start));
    }

    private void emitNulEscapeIfNeeded(char quote, int value, SourcePosition start) {
      if (value == 0) {
        emitNulIfStringLiteral(quote, start);
      }
    }

    private void emitNulIfStringLiteral(char quote, SourcePosition start) {
      if (quote == '"') {
        emitDiagnostic(
            IdlDiagnosticCodes.NUL_LITERAL_CHARACTER,
            "IDL string literals must not contain NUL",
            start,
            currentDiagnosticEnd(start));
      }
    }

    private int consumeDecimalDigits() {
      int count = 0;
      while (isDecimalDigit(peek())) {
        advance();
        count++;
      }
      return count;
    }

    private boolean consumeInvalidNumericTail() {
      boolean invalid = false;
      while (isIdentifierPart(peek())) {
        advance();
        invalid = true;
      }
      return invalid;
    }

    private void consumeIdentifierTail() {
      while (isIdentifierPart(peek())) {
        advance();
      }
    }

    private boolean containsInvalidOctalDigit(int startInclusive, int endExclusive) {
      if (endExclusive - startInclusive <= 1 || source.charAt(startInclusive) != '0') {
        return false;
      }
      for (int position = startInclusive + 1; position < endExclusive; position++) {
        char current = source.charAt(position);
        if (current == '8' || current == '9') {
          return true;
        }
      }
      return false;
    }

    private String lexemeFrom(SourcePosition start) {
      return source.substring(Math.toIntExact(start.offset()), index);
    }

    private SourceSpan span(SourcePosition start, SourcePosition end) {
      return new SourceSpan(start, end);
    }

    private SourcePosition currentDiagnosticEnd(SourcePosition start) {
      return index == Math.toIntExact(start.offset()) ? start : lastConsumedPosition;
    }

    private SourcePosition currentPosition() {
      return new SourcePosition(sourceName, line, column, index);
    }

    private boolean startsWith(String expected) {
      return source.startsWith(expected, index);
    }

    private boolean isAtEnd() {
      return index >= source.length();
    }

    private char peek() {
      return isAtEnd() ? '\0' : source.charAt(index);
    }

    private char peekNext() {
      return index + 1 >= source.length() ? '\0' : source.charAt(index + 1);
    }

    private char advance() {
      lastConsumedPosition = currentPosition();
      char current = source.charAt(index);
      if (current == '\r') {
        if (index + 1 < source.length() && source.charAt(index + 1) == '\n') {
          index += 2;
        } else {
          index++;
        }
        line++;
        column = 1;
      } else if (current == '\n') {
        index++;
        line++;
        column = 1;
      } else {
        index++;
        column++;
      }
      return current;
    }

    private static IdlTokenKind literalKind(boolean wide, char quote) {
      if (quote == '\'') {
        return wide ? IdlTokenKind.WIDE_CHARACTER_LITERAL : IdlTokenKind.CHARACTER_LITERAL;
      }
      return wide ? IdlTokenKind.WIDE_STRING_LITERAL : IdlTokenKind.STRING_LITERAL;
    }

    private static boolean isWhitespace(char value) {
      return value == ' '
          || value == '\t'
          || value == '\n'
          || value == '\r'
          || value == '\f'
          || value == '\u000B';
    }

    private static boolean isSimpleEscape(char value) {
      return value == 'n'
          || value == 't'
          || value == 'v'
          || value == 'b'
          || value == 'r'
          || value == 'f'
          || value == 'a'
          || value == '\\'
          || value == '?'
          || value == '\''
          || value == '"';
    }

    private static boolean isAsciiLetter(char value) {
      return (value >= 'A' && value <= 'Z') || (value >= 'a' && value <= 'z');
    }

    private static boolean isDecimalDigit(char value) {
      return value >= '0' && value <= '9';
    }

    private static boolean isOctalDigit(char value) {
      return value >= '0' && value <= '7';
    }

    private static boolean isHexDigit(char value) {
      return (value >= '0' && value <= '9')
          || (value >= 'A' && value <= 'F')
          || (value >= 'a' && value <= 'f');
    }

    private static boolean isIdentifierPart(char value) {
      return isAsciiLetter(value) || isDecimalDigit(value) || value == '_';
    }

    private static String requireNonBlank(String value, String name) {
      Objects.requireNonNull(value, name);
      if (value.isBlank()) {
        throw new IllegalArgumentException(name + " must not be blank");
      }
      return value;
    }
  }
}
