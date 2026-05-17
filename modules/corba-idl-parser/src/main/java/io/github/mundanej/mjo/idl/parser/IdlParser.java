package io.github.mundanej.mjo.idl.parser;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import io.github.mundanej.mjo.common.SourceSpan;
import io.github.mundanej.mjo.idl.ast.IdlAttribute;
import io.github.mundanej.mjo.idl.ast.IdlConstant;
import io.github.mundanej.mjo.idl.ast.IdlConstantExpression;
import io.github.mundanej.mjo.idl.ast.IdlDeclaration;
import io.github.mundanej.mjo.idl.ast.IdlEnum;
import io.github.mundanej.mjo.idl.ast.IdlExceptionDeclaration;
import io.github.mundanej.mjo.idl.ast.IdlField;
import io.github.mundanej.mjo.idl.ast.IdlInterface;
import io.github.mundanej.mjo.idl.ast.IdlInterfaceMember;
import io.github.mundanej.mjo.idl.ast.IdlModule;
import io.github.mundanej.mjo.idl.ast.IdlOperation;
import io.github.mundanej.mjo.idl.ast.IdlParameter;
import io.github.mundanej.mjo.idl.ast.IdlParameterDirection;
import io.github.mundanej.mjo.idl.ast.IdlStruct;
import io.github.mundanej.mjo.idl.ast.IdlTranslationUnit;
import io.github.mundanej.mjo.idl.ast.IdlTypeReference;
import io.github.mundanej.mjo.idl.lexer.IdlToken;
import io.github.mundanej.mjo.idl.lexer.IdlTokenKind;
import io.github.mundanej.mjo.idl.preprocessor.IdlPreprocessResult;
import io.github.mundanej.mjo.idl.preprocessor.IdlPreprocessor;
import io.github.mundanej.mjo.idl.preprocessor.IdlSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Parser for the approved minimal OMG IDL grammar slice. */
public final class IdlParser {

  private final IdlPreprocessor preprocessor;

  /** Creates a parser that uses the default bounded preprocessor. */
  public IdlParser() {
    this(new IdlPreprocessor());
  }

  /** Creates a parser that uses the supplied preprocessor. */
  public IdlParser(IdlPreprocessor preprocessor) {
    this.preprocessor = Objects.requireNonNull(preprocessor, "preprocessor");
  }

  /** Parses IDL source text using the default source wrapper. */
  public IdlParseResult parse(String sourceName, String source) {
    return parse(new IdlSource(sourceName, source));
  }

  /** Preprocesses and parses one IDL source. */
  public IdlParseResult parse(IdlSource source) {
    return parse(preprocessor.preprocess(source));
  }

  /** Parses a caller-supplied preprocessed token stream. */
  public IdlParseResult parse(IdlPreprocessResult preprocessResult) {
    Objects.requireNonNull(preprocessResult, "preprocessResult");
    if (preprocessResult.hasErrors()) {
      return new IdlParseResult(Optional.empty(), preprocessResult.diagnostics());
    }
    return new Parser(preprocessResult.tokens(), preprocessResult.diagnostics()).parse();
  }

  private static final class Parser {

    private static final Set<String> UNSUPPORTED_DECLARATION_KEYWORDS =
        Set.of(
            "abstract",
            "alias",
            "bitfield",
            "bitmask",
            "bitset",
            "component",
            "connector",
            "custom",
            "eventtype",
            "home",
            "import",
            "local",
            "native",
            "porttype",
            "typedef",
            "typeid",
            "typename",
            "typeprefix",
            "union",
            "valuetype");
    private static final Set<String> UNSUPPORTED_TYPE_KEYWORDS = Set.of("fixed", "map", "sequence");
    private static final Set<String> SINGLE_TOKEN_TYPE_KEYWORDS =
        Set.of(
            "any",
            "boolean",
            "char",
            "double",
            "float",
            "int8",
            "int16",
            "int32",
            "int64",
            "Object",
            "octet",
            "short",
            "string",
            "uint8",
            "uint16",
            "uint32",
            "uint64",
            "ValueBase",
            "wchar",
            "wstring");

    private final List<IdlToken> tokens;
    private final List<Diagnostic> diagnostics;
    private int current;

    private Parser(List<IdlToken> tokens, List<Diagnostic> diagnostics) {
      this.tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
      if (this.tokens.isEmpty() || this.tokens.getLast().kind() != IdlTokenKind.END_OF_FILE) {
        throw new IllegalArgumentException("tokens must include a final EOF token");
      }
      this.diagnostics = new ArrayList<>(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    private IdlParseResult parse() {
      List<IdlDeclaration> declarations = new ArrayList<>();
      while (!isAtEnd()) {
        IdlDeclaration declaration = parseDeclaration();
        if (declaration != null) {
          declarations.add(declaration);
        }
      }
      if (hasErrors()) {
        return new IdlParseResult(Optional.empty(), diagnostics);
      }
      IdlTranslationUnit translationUnit =
          new IdlTranslationUnit(declarations, translationUnitSpan());
      return new IdlParseResult(Optional.of(translationUnit), diagnostics);
    }

    private IdlDeclaration parseDeclaration() {
      if (check(IdlTokenKind.HASH)) {
        emitUnsupported(
            "preprocessor directives are not part of the minimal parser slice", current());
        synchronizeDeclaration();
        return null;
      }
      if (matchKeyword("module")) {
        return parseModule(previous());
      }
      if (matchKeyword("interface")) {
        return parseInterface(previous());
      }
      if (matchKeyword("struct")) {
        return parseStruct(previous());
      }
      if (matchKeyword("enum")) {
        return parseEnum(previous());
      }
      if (matchKeyword("exception")) {
        return parseException(previous());
      }
      if (matchKeyword("const")) {
        return parseConstant(previous());
      }
      if (isUnsupportedDeclarationStart(current())) {
        emitUnsupported("unsupported declaration: " + current().lexeme(), current());
        synchronizeDeclaration();
        return null;
      }

      emitUnexpected("declaration");
      synchronizeDeclaration();
      return null;
    }

    private IdlModule parseModule(IdlToken start) {
      IdlToken name = consumeIdentifier("module name");
      if (name == null || consume(IdlTokenKind.LEFT_BRACE, "'{' after module name") == null) {
        synchronizeDeclaration();
        return null;
      }

      List<IdlDeclaration> declarations = new ArrayList<>();
      while (!check(IdlTokenKind.RIGHT_BRACE) && !isAtEnd()) {
        IdlDeclaration declaration = parseDeclaration();
        if (declaration != null) {
          declarations.add(declaration);
        }
      }

      IdlToken close = consume(IdlTokenKind.RIGHT_BRACE, "'}' after module body");
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after module declaration");
      if (close == null || end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlModule(identifierText(name), declarations, span(start, end));
    }

    private IdlInterface parseInterface(IdlToken start) {
      IdlToken name = consumeIdentifier("interface name");
      if (name == null) {
        synchronizeDeclaration();
        return null;
      }
      if (match(IdlTokenKind.SEMICOLON)) {
        emitUnsupported(
            "interface forward declarations are not part of the minimal parser slice", name);
        return null;
      }
      if (match(IdlTokenKind.COLON)) {
        emitUnsupported(
            "interface inheritance is not part of the minimal parser slice", previous());
        synchronizeDeclaration();
        return null;
      }
      if (consume(IdlTokenKind.LEFT_BRACE, "'{' after interface name") == null) {
        synchronizeDeclaration();
        return null;
      }

      List<IdlInterfaceMember> members = new ArrayList<>();
      while (!check(IdlTokenKind.RIGHT_BRACE) && !isAtEnd()) {
        members.addAll(parseInterfaceMember());
      }

      IdlToken close = consume(IdlTokenKind.RIGHT_BRACE, "'}' after interface body");
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after interface declaration");
      if (close == null || end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlInterface(identifierText(name), members, span(start, end));
    }

    private List<IdlInterfaceMember> parseInterfaceMember() {
      if (matchKeyword("readonly")) {
        IdlAttribute attribute = parseAttribute(previous(), true);
        return attribute == null ? List.of() : List.of(attribute);
      }
      if (matchKeyword("attribute")) {
        IdlAttribute attribute = parseAttribute(previous(), false);
        return attribute == null ? List.of() : List.of(attribute);
      }
      if (startsType(true) || checkKeyword("oneway")) {
        IdlOperation operation = parseOperation();
        return operation == null ? List.of() : List.of(operation);
      }
      if (isUnsupportedDeclarationStart(current())) {
        emitUnsupported("unsupported interface member: " + current().lexeme(), current());
        synchronizeDeclaration();
        return List.of();
      }

      emitUnexpected("interface member");
      synchronizeDeclaration();
      return List.of();
    }

    private IdlAttribute parseAttribute(IdlToken start, boolean readonly) {
      if (readonly && consumeKeyword("attribute", "'attribute' after readonly") == null) {
        synchronizeDeclaration();
        return null;
      }
      IdlTypeReference type = parseType(false);
      if (type == null) {
        synchronizeDeclaration();
        return null;
      }
      List<String> names = new ArrayList<>();
      IdlToken name = parseSimpleDeclaratorName();
      if (name == null) {
        synchronizeDeclaration();
        return null;
      }
      names.add(identifierText(name));
      while (match(IdlTokenKind.COMMA)) {
        name = parseSimpleDeclaratorName();
        if (name == null) {
          synchronizeDeclaration();
          return null;
        }
        names.add(identifierText(name));
      }
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after attribute declaration");
      if (end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlAttribute(readonly, type, names, span(start, end));
    }

    private IdlOperation parseOperation() {
      IdlToken start = current();
      boolean oneway = matchKeyword("oneway");
      if (oneway) {
        start = previous();
      }
      IdlTypeReference returnType = parseType(true);
      if (returnType == null) {
        synchronizeDeclaration();
        return null;
      }
      IdlToken name = consumeIdentifier("operation name");
      if (name == null || consume(IdlTokenKind.LEFT_PAREN, "'(' after operation name") == null) {
        synchronizeDeclaration();
        return null;
      }

      List<IdlParameter> parameters = new ArrayList<>();
      if (!check(IdlTokenKind.RIGHT_PAREN)) {
        do {
          IdlParameter parameter = parseParameter();
          if (parameter == null) {
            synchronizeDeclaration();
            return null;
          }
          parameters.add(parameter);
        } while (match(IdlTokenKind.COMMA));
      }
      if (consume(IdlTokenKind.RIGHT_PAREN, "')' after operation parameters") == null) {
        synchronizeDeclaration();
        return null;
      }

      List<String> raises = List.of();
      if (matchKeyword("raises")) {
        raises = parseRaisesClause();
        if (raises == null) {
          synchronizeDeclaration();
          return null;
        }
      }
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after operation declaration");
      if (end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlOperation(
          oneway, returnType, identifierText(name), parameters, raises, span(start, end));
    }

    private IdlParameter parseParameter() {
      IdlToken start = current();
      IdlParameterDirection direction = parseParameterDirection();
      if (direction == null) {
        return null;
      }
      IdlTypeReference type = parseType(false);
      IdlToken name = consumeIdentifier("parameter name");
      if (type == null || name == null) {
        return null;
      }
      return new IdlParameter(direction, type, identifierText(name), span(start, name));
    }

    private IdlParameterDirection parseParameterDirection() {
      if (matchKeyword("in")) {
        return IdlParameterDirection.IN;
      }
      if (matchKeyword("out")) {
        return IdlParameterDirection.OUT;
      }
      if (matchKeyword("inout")) {
        return IdlParameterDirection.INOUT;
      }
      emitUnexpected("parameter direction");
      return null;
    }

    private List<String> parseRaisesClause() {
      if (consume(IdlTokenKind.LEFT_PAREN, "'(' after raises") == null) {
        return null;
      }
      List<String> names = new ArrayList<>();
      do {
        ScopedName scopedName = parseScopedName();
        if (scopedName == null) {
          return null;
        }
        names.add(scopedName.name());
      } while (match(IdlTokenKind.COMMA));
      if (consume(IdlTokenKind.RIGHT_PAREN, "')' after raises clause") == null) {
        return null;
      }
      return names;
    }

    private IdlStruct parseStruct(IdlToken start) {
      IdlToken name = consumeIdentifier("struct name");
      if (name == null || consume(IdlTokenKind.LEFT_BRACE, "'{' after struct name") == null) {
        synchronizeDeclaration();
        return null;
      }
      List<IdlField> fields = parseFieldsUntilRightBrace();
      IdlToken close = consume(IdlTokenKind.RIGHT_BRACE, "'}' after struct body");
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after struct declaration");
      if (fields == null || close == null || end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlStruct(identifierText(name), fields, span(start, end));
    }

    private IdlExceptionDeclaration parseException(IdlToken start) {
      IdlToken name = consumeIdentifier("exception name");
      if (name == null || consume(IdlTokenKind.LEFT_BRACE, "'{' after exception name") == null) {
        synchronizeDeclaration();
        return null;
      }
      List<IdlField> fields = parseFieldsUntilRightBrace();
      IdlToken close = consume(IdlTokenKind.RIGHT_BRACE, "'}' after exception body");
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after exception declaration");
      if (fields == null || close == null || end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlExceptionDeclaration(identifierText(name), fields, span(start, end));
    }

    private List<IdlField> parseFieldsUntilRightBrace() {
      List<IdlField> fields = new ArrayList<>();
      while (!check(IdlTokenKind.RIGHT_BRACE) && !isAtEnd()) {
        List<IdlField> fieldGroup = parseFieldGroup();
        if (fieldGroup == null) {
          return null;
        }
        fields.addAll(fieldGroup);
      }
      return fields;
    }

    private List<IdlField> parseFieldGroup() {
      IdlTypeReference type = parseType(false);
      if (type == null) {
        synchronizeDeclaration();
        return null;
      }
      List<IdlField> fields = new ArrayList<>();
      IdlToken name = parseSimpleDeclaratorName();
      if (name == null) {
        synchronizeDeclaration();
        return null;
      }
      fields.add(new IdlField(type, identifierText(name), span(type.span(), name)));
      while (match(IdlTokenKind.COMMA)) {
        name = parseSimpleDeclaratorName();
        if (name == null) {
          synchronizeDeclaration();
          return null;
        }
        fields.add(new IdlField(type, identifierText(name), span(type.span(), name)));
      }
      if (consume(IdlTokenKind.SEMICOLON, "';' after field declaration") == null) {
        synchronizeDeclaration();
        return null;
      }
      return fields;
    }

    private IdlEnum parseEnum(IdlToken start) {
      IdlToken name = consumeIdentifier("enum name");
      if (name == null || consume(IdlTokenKind.LEFT_BRACE, "'{' after enum name") == null) {
        synchronizeDeclaration();
        return null;
      }
      List<String> enumerators = new ArrayList<>();
      IdlToken enumerator = consumeIdentifier("enum enumerator");
      if (enumerator == null) {
        synchronizeDeclaration();
        return null;
      }
      enumerators.add(identifierText(enumerator));
      while (match(IdlTokenKind.COMMA)) {
        enumerator = consumeIdentifier("enum enumerator");
        if (enumerator == null) {
          synchronizeDeclaration();
          return null;
        }
        enumerators.add(identifierText(enumerator));
      }
      IdlToken close = consume(IdlTokenKind.RIGHT_BRACE, "'}' after enum body");
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after enum declaration");
      if (close == null || end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlEnum(identifierText(name), enumerators, span(start, end));
    }

    private IdlConstant parseConstant(IdlToken start) {
      IdlTypeReference type = parseType(false);
      IdlToken name = consumeIdentifier("constant name");
      if (type == null
          || name == null
          || consume(IdlTokenKind.EQUALS, "'=' after constant name") == null) {
        synchronizeDeclaration();
        return null;
      }
      IdlConstantExpression expression = parseConstantExpression();
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after constant declaration");
      if (expression == null || end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlConstant(type, identifierText(name), expression, span(start, end));
    }

    private IdlConstantExpression parseConstantExpression() {
      if (check(IdlTokenKind.SEMICOLON)) {
        emitUnexpected("constant expression");
        return null;
      }
      IdlToken start = current();
      IdlToken last = start;
      int parenDepth = 0;
      int bracketDepth = 0;
      List<String> lexemes = new ArrayList<>();
      while (!isAtEnd()) {
        if (parenDepth == 0 && bracketDepth == 0 && check(IdlTokenKind.SEMICOLON)) {
          break;
        }
        if (match(IdlTokenKind.LEFT_PAREN)) {
          parenDepth++;
        } else if (match(IdlTokenKind.RIGHT_PAREN)) {
          parenDepth = Math.max(0, parenDepth - 1);
        } else if (match(IdlTokenKind.LEFT_BRACKET)) {
          bracketDepth++;
        } else if (match(IdlTokenKind.RIGHT_BRACKET)) {
          bracketDepth = Math.max(0, bracketDepth - 1);
        } else {
          advance();
        }
        last = previous();
        lexemes.add(last.lexeme());
      }
      if (isAtEnd()) {
        emitUnexpected("';' after constant expression");
        return null;
      }
      return new IdlConstantExpression(lexemes, span(start, last));
    }

    private IdlTypeReference parseType(boolean allowVoid) {
      IdlToken start = current();
      if (allowVoid && matchKeyword("void")) {
        return new IdlTypeReference("void", span(start, previous()));
      }
      if (matchKeyword("unsigned")) {
        return parseUnsignedType(start);
      }
      if (matchKeyword("long")) {
        return parseLongType(start);
      }
      String primitiveType = singleTokenTypeName(current());
      if (primitiveType != null) {
        advance();
        IdlToken end = previous();
        if (check(IdlTokenKind.LESS_THAN)) {
          emitUnsupportedType(
              "bounded or parameterized type is not part of the minimal parser slice", current());
          return null;
        }
        return new IdlTypeReference(primitiveType, span(start, end));
      }
      if (isUnsupportedTypeStart(current())) {
        emitUnsupportedType("unsupported type: " + current().lexeme(), current());
        return null;
      }
      ScopedName scopedName = parseScopedName();
      if (scopedName == null) {
        return null;
      }
      return new IdlTypeReference(scopedName.name(), scopedName.span());
    }

    private IdlTypeReference parseUnsignedType(IdlToken start) {
      if (matchKeyword("short")) {
        return new IdlTypeReference("unsigned short", span(start, previous()));
      }
      if (matchKeyword("long")) {
        if (matchKeyword("long")) {
          return new IdlTypeReference("unsigned long long", span(start, previous()));
        }
        return new IdlTypeReference("unsigned long", span(start, previous()));
      }
      emitUnexpected("unsigned integer type");
      return null;
    }

    private IdlTypeReference parseLongType(IdlToken start) {
      if (matchKeyword("long")) {
        return new IdlTypeReference("long long", span(start, previous()));
      }
      if (matchKeyword("double")) {
        return new IdlTypeReference("long double", span(start, previous()));
      }
      return new IdlTypeReference("long", span(start, previous()));
    }

    private ScopedName parseScopedName() {
      IdlToken start = current();
      StringBuilder name = new StringBuilder();
      if (match(IdlTokenKind.DOUBLE_COLON)) {
        name.append("::");
      }
      IdlToken segment = consumeIdentifier("scoped name segment");
      if (segment == null) {
        return null;
      }
      name.append(identifierText(segment));
      IdlToken end = segment;
      while (match(IdlTokenKind.DOUBLE_COLON)) {
        segment = consumeIdentifier("scoped name segment");
        if (segment == null) {
          return null;
        }
        name.append("::").append(identifierText(segment));
        end = segment;
      }
      return new ScopedName(name.toString(), span(start, end));
    }

    private IdlToken parseSimpleDeclaratorName() {
      IdlToken name = consumeIdentifier("declarator name");
      if (name == null) {
        return null;
      }
      if (check(IdlTokenKind.LEFT_BRACKET)) {
        emitDiagnostic(
            IdlParserDiagnosticCodes.UNSUPPORTED_DECLARATOR,
            "array declarators are not part of the minimal parser slice",
            current().span());
        return null;
      }
      return name;
    }

    private IdlToken consumeIdentifier(String label) {
      if (check(IdlTokenKind.IDENTIFIER) || check(IdlTokenKind.ESCAPED_IDENTIFIER)) {
        return advance();
      }
      emitUnexpected(label);
      return null;
    }

    private IdlToken consume(IdlTokenKind kind, String expectation) {
      if (check(kind)) {
        return advance();
      }
      emitUnexpected(expectation);
      return null;
    }

    private IdlToken consumeKeyword(String keyword, String expectation) {
      if (checkKeyword(keyword)) {
        return advance();
      }
      emitUnexpected(expectation);
      return null;
    }

    private boolean startsType(boolean allowVoid) {
      return (allowVoid && checkKeyword("void"))
          || checkKeyword("unsigned")
          || checkKeyword("long")
          || singleTokenTypeName(current()) != null
          || isUnsupportedTypeStart(current())
          || check(IdlTokenKind.IDENTIFIER)
          || check(IdlTokenKind.ESCAPED_IDENTIFIER)
          || check(IdlTokenKind.DOUBLE_COLON);
    }

    private boolean isUnsupportedDeclarationStart(IdlToken token) {
      return token.kind() == IdlTokenKind.KEYWORD
          && UNSUPPORTED_DECLARATION_KEYWORDS.contains(token.lexeme());
    }

    private boolean isUnsupportedTypeStart(IdlToken token) {
      return token.kind() == IdlTokenKind.KEYWORD
          && UNSUPPORTED_TYPE_KEYWORDS.contains(token.lexeme());
    }

    private static String singleTokenTypeName(IdlToken token) {
      return token.kind() == IdlTokenKind.KEYWORD
              && SINGLE_TOKEN_TYPE_KEYWORDS.contains(token.lexeme())
          ? token.lexeme()
          : null;
    }

    private boolean match(IdlTokenKind kind) {
      if (!check(kind)) {
        return false;
      }
      advance();
      return true;
    }

    private boolean matchKeyword(String keyword) {
      if (!checkKeyword(keyword)) {
        return false;
      }
      advance();
      return true;
    }

    private boolean check(IdlTokenKind kind) {
      return current().kind() == kind;
    }

    private boolean checkKeyword(String keyword) {
      return current().kind() == IdlTokenKind.KEYWORD && current().lexeme().equals(keyword);
    }

    private IdlToken advance() {
      if (!isAtEnd()) {
        current++;
      }
      return previous();
    }

    private boolean isAtEnd() {
      return current().kind() == IdlTokenKind.END_OF_FILE;
    }

    private IdlToken current() {
      return tokens.get(Math.min(current, tokens.size() - 1));
    }

    private IdlToken previous() {
      return tokens.get(current - 1);
    }

    private void synchronizeDeclaration() {
      while (!isAtEnd()) {
        if (check(IdlTokenKind.SEMICOLON)) {
          advance();
          return;
        }
        if (check(IdlTokenKind.RIGHT_BRACE)) {
          return;
        }
        advance();
      }
    }

    private void emitUnexpected(String expectation) {
      IdlToken token = current();
      DiagnosticCode code =
          token.kind() == IdlTokenKind.END_OF_FILE
              ? IdlParserDiagnosticCodes.UNEXPECTED_EOF
              : IdlParserDiagnosticCodes.UNEXPECTED_TOKEN;
      emitDiagnostic(
          code, "Expected " + expectation + " before " + tokenDescription(token), token.span());
    }

    private void emitUnsupported(String message, IdlToken token) {
      emitDiagnostic(IdlParserDiagnosticCodes.UNSUPPORTED_CONSTRUCT, message, token.span());
    }

    private void emitUnsupportedType(String message, IdlToken token) {
      emitDiagnostic(IdlParserDiagnosticCodes.UNSUPPORTED_TYPE, message, token.span());
    }

    private void emitDiagnostic(DiagnosticCode code, String message, SourceSpan span) {
      diagnostics.add(Diagnostic.withSpan(code, DiagnosticSeverity.ERROR, message, span));
    }

    private boolean hasErrors() {
      return diagnostics.stream()
          .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
    }

    private SourceSpan translationUnitSpan() {
      IdlToken eof = tokens.getLast();
      if (tokens.size() == 1) {
        return eof.span();
      }
      IdlToken first = tokens.getFirst();
      IdlToken last = tokens.get(tokens.size() - 2);
      if (!first.span().start().sourceName().equals(last.span().end().sourceName())) {
        return first.span();
      }
      return new SourceSpan(first.span().start(), last.span().end());
    }

    private static SourceSpan span(IdlToken start, IdlToken end) {
      return new SourceSpan(start.span().start(), end.span().end());
    }

    private static SourceSpan span(SourceSpan start, IdlToken end) {
      return new SourceSpan(start.start(), end.span().end());
    }

    private static String identifierText(IdlToken token) {
      return token.identifierText().orElseThrow();
    }

    private static String tokenDescription(IdlToken token) {
      return token.kind() == IdlTokenKind.END_OF_FILE
          ? "end of source"
          : "'" + token.lexeme() + "'";
    }
  }

  private record ScopedName(String name, SourceSpan span) {}
}
