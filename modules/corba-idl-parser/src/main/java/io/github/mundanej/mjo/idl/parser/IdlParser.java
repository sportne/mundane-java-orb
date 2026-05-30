package io.github.mundanej.mjo.idl.parser;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import io.github.mundanej.mjo.common.SourceSpan;
import io.github.mundanej.mjo.idl.ast.IdlArrayDimension;
import io.github.mundanej.mjo.idl.ast.IdlAttribute;
import io.github.mundanej.mjo.idl.ast.IdlConstant;
import io.github.mundanej.mjo.idl.ast.IdlConstantExpression;
import io.github.mundanej.mjo.idl.ast.IdlDeclaration;
import io.github.mundanej.mjo.idl.ast.IdlDeclarator;
import io.github.mundanej.mjo.idl.ast.IdlEnum;
import io.github.mundanej.mjo.idl.ast.IdlExceptionDeclaration;
import io.github.mundanej.mjo.idl.ast.IdlField;
import io.github.mundanej.mjo.idl.ast.IdlInterface;
import io.github.mundanej.mjo.idl.ast.IdlInterfaceForward;
import io.github.mundanej.mjo.idl.ast.IdlInterfaceKind;
import io.github.mundanej.mjo.idl.ast.IdlInterfaceMember;
import io.github.mundanej.mjo.idl.ast.IdlModule;
import io.github.mundanej.mjo.idl.ast.IdlNative;
import io.github.mundanej.mjo.idl.ast.IdlOperation;
import io.github.mundanej.mjo.idl.ast.IdlParameter;
import io.github.mundanej.mjo.idl.ast.IdlParameterDirection;
import io.github.mundanej.mjo.idl.ast.IdlPragma;
import io.github.mundanej.mjo.idl.ast.IdlStruct;
import io.github.mundanej.mjo.idl.ast.IdlTranslationUnit;
import io.github.mundanej.mjo.idl.ast.IdlTypeReference;
import io.github.mundanej.mjo.idl.ast.IdlTypedef;
import io.github.mundanej.mjo.idl.ast.IdlUnion;
import io.github.mundanej.mjo.idl.ast.IdlUnionCase;
import io.github.mundanej.mjo.idl.ast.IdlUnionLabel;
import io.github.mundanej.mjo.idl.ast.IdlValueBox;
import io.github.mundanej.mjo.idl.ast.IdlValueFactory;
import io.github.mundanej.mjo.idl.ast.IdlValueField;
import io.github.mundanej.mjo.idl.ast.IdlValueMember;
import io.github.mundanej.mjo.idl.ast.IdlValueType;
import io.github.mundanej.mjo.idl.ast.IdlValueTypeForward;
import io.github.mundanej.mjo.idl.ast.IdlValueVisibility;
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
            "alias",
            "bitfield",
            "bitmask",
            "bitset",
            "component",
            "connector",
            "eventtype",
            "home",
            "import",
            "porttype",
            "typename");
    private static final Set<String> UNSUPPORTED_TYPE_KEYWORDS = Set.of("fixed", "map");
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
    private int pendingGreaterThanClosers;

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
        return parseHashPragma();
      }
      if (matchKeyword("module")) {
        return parseModule(previous());
      }
      if (matchKeyword("abstract")) {
        IdlToken start = previous();
        if (matchKeyword("interface")) {
          return parseInterface(start, IdlInterfaceKind.ABSTRACT);
        }
        if (matchKeyword("valuetype")) {
          return parseValueType(start, false, true);
        }
        emitUnexpected("interface or valuetype after abstract");
        synchronizeDeclaration();
        return null;
      }
      if (matchKeyword("local")) {
        IdlToken start = previous();
        if (matchKeyword("interface")) {
          return parseInterface(start, IdlInterfaceKind.LOCAL);
        }
        emitUnexpected("interface after local");
        synchronizeDeclaration();
        return null;
      }
      if (matchKeyword("custom")) {
        IdlToken start = previous();
        if (consumeKeyword("valuetype", "'valuetype' after custom") == null) {
          synchronizeDeclaration();
          return null;
        }
        return parseValueType(start, true, false);
      }
      if (matchKeyword("interface")) {
        return parseInterface(previous(), IdlInterfaceKind.NORMAL);
      }
      if (matchKeyword("valuetype")) {
        return parseValueType(previous(), false, false);
      }
      if (matchKeyword("native")) {
        return parseNative(previous());
      }
      if (checkKeyword("typeid") || checkKeyword("typeprefix")) {
        return parseRepositoryPragma(advance());
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
      if (matchKeyword("typedef")) {
        return parseTypedef(previous());
      }
      if (matchKeyword("union")) {
        return parseUnion(previous());
      }
      if (isUnsupportedDeclarationStart(current())) {
        emitUnsupported("unsupported declaration: " + current().lexeme(), current());
        synchronizeDeclaration();
        return null;
      }

      emitUnexpected("declaration");
      if (check(IdlTokenKind.RIGHT_BRACE)) {
        advance();
        return null;
      }
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

    private IdlDeclaration parseInterface(IdlToken start, IdlInterfaceKind kind) {
      IdlToken name = consumeIdentifier("interface name");
      if (name == null) {
        synchronizeDeclaration();
        return null;
      }
      if (match(IdlTokenKind.SEMICOLON)) {
        return new IdlInterfaceForward(kind, identifierText(name), span(start, previous()));
      }
      List<String> bases = List.of();
      if (match(IdlTokenKind.COLON)) {
        bases = parseInterfaceBases();
        if (bases == null) {
          synchronizeDeclaration();
          return null;
        }
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
      return new IdlInterface(kind, identifierText(name), bases, members, span(start, end));
    }

    private IdlDeclaration parseValueType(IdlToken start, boolean custom, boolean abstractValue) {
      IdlToken name = consumeIdentifier("valuetype name");
      if (name == null) {
        synchronizeDeclaration();
        return null;
      }
      if (match(IdlTokenKind.SEMICOLON)) {
        return new IdlValueTypeForward(
            abstractValue, identifierText(name), span(start, previous()));
      }
      if (!abstractValue && !custom && startsType(false)) {
        IdlTypeReference boxedType = parseType(false);
        IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after value box declaration");
        if (boxedType == null || end == null) {
          synchronizeDeclaration();
          return null;
        }
        return new IdlValueBox(identifierText(name), boxedType, span(start, end));
      }

      List<String> bases = List.of();
      if (match(IdlTokenKind.COLON)) {
        matchKeyword("truncatable");
        bases = parseInterfaceBases();
        if (bases == null) {
          synchronizeDeclaration();
          return null;
        }
      }
      List<String> supported = List.of();
      if (matchKeyword("supports")) {
        supported = parseInterfaceBases();
        if (supported == null) {
          synchronizeDeclaration();
          return null;
        }
      }
      if (consume(IdlTokenKind.LEFT_BRACE, "'{' after valuetype name") == null) {
        synchronizeDeclaration();
        return null;
      }
      List<IdlValueMember> members = new ArrayList<>();
      while (!check(IdlTokenKind.RIGHT_BRACE) && !isAtEnd()) {
        members.addAll(parseValueMember());
      }
      IdlToken close = consume(IdlTokenKind.RIGHT_BRACE, "'}' after valuetype body");
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after valuetype declaration");
      if (close == null || end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlValueType(
          custom, abstractValue, identifierText(name), bases, supported, members, span(start, end));
    }

    private IdlNative parseNative(IdlToken start) {
      IdlToken name = consumeIdentifier("native name");
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after native declaration");
      if (name == null || end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlNative(identifierText(name), span(start, end));
    }

    private IdlPragma parseRepositoryPragma(IdlToken start) {
      List<String> arguments = new ArrayList<>();
      ScopedName target = parseScopedName();
      if (target != null) {
        arguments.add(target.name());
      }
      if (isStringLiteral(current())) {
        arguments.add(advance().lexeme());
      } else {
        emitUnexpected("repository pragma string literal");
      }
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after repository pragma");
      if (target == null || arguments.size() < 2 || end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlPragma(start.lexeme(), arguments, span(start, end));
    }

    private IdlPragma parseHashPragma() {
      IdlToken start = advance();
      int line = start.span().start().line();
      if (!sameLine(line) || !tokenText(current()).equals("pragma")) {
        emitUnsupported("unsupported preprocessor directive in parser input", start);
        synchronizeDeclaration();
        return null;
      }
      advance();
      if (!sameLine(line) || !identifierLike(current())) {
        emitUnexpected("pragma name");
        synchronizeDeclaration();
        return null;
      }
      IdlToken name = advance();
      if ((tokenText(name).equals("ID") || tokenText(name).equals("version")) && sameLine(line)) {
        return parseHashRepositoryPragma(start, name, line);
      }
      List<String> arguments = new ArrayList<>();
      IdlToken end = name;
      while (!isAtEnd() && sameLine(line)) {
        arguments.add(tokenText(advance()));
        end = previous();
      }
      return new IdlPragma(tokenText(name), arguments, span(start, end));
    }

    private IdlPragma parseHashRepositoryPragma(IdlToken start, IdlToken name, int line) {
      List<String> arguments = new ArrayList<>();
      if (!sameLine(line)) {
        emitUnexpected("repository pragma target");
        synchronizeDeclaration();
        return null;
      }
      ScopedName target = parseScopedName();
      if (target != null) {
        arguments.add(target.name());
      }
      if (!sameLine(line) || isAtEnd()) {
        emitUnexpected("repository pragma value");
        synchronizeDeclaration();
        return null;
      }
      IdlToken value = advance();
      arguments.add(tokenText(value));
      return target == null ? null : new IdlPragma(tokenText(name), arguments, span(start, value));
    }

    private List<String> parseInterfaceBases() {
      List<String> bases = new ArrayList<>();
      do {
        ScopedName scopedName = parseScopedName();
        if (scopedName == null) {
          return null;
        }
        bases.add(scopedName.name());
      } while (match(IdlTokenKind.COMMA));
      return bases;
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

    private List<IdlValueMember> parseValueMember() {
      if (matchKeyword("public")) {
        IdlValueField field = parseValueField(previous(), IdlValueVisibility.PUBLIC);
        return field == null ? List.of() : List.of(field);
      }
      if (matchKeyword("private")) {
        IdlValueField field = parseValueField(previous(), IdlValueVisibility.PRIVATE);
        return field == null ? List.of() : List.of(field);
      }
      if (matchKeyword("factory")) {
        IdlValueFactory factory = parseValueFactory(previous());
        return factory == null ? List.of() : List.of(factory);
      }
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

      emitUnexpected("valuetype member");
      synchronizeDeclaration();
      return List.of();
    }

    private IdlValueField parseValueField(IdlToken start, IdlValueVisibility visibility) {
      IdlTypeReference type = parseType(false);
      List<IdlDeclarator> declarators = parseDeclarators();
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after valuetype state member");
      if (type == null || declarators == null || end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlValueField(visibility, type, declarators, span(start, end));
    }

    private IdlValueFactory parseValueFactory(IdlToken start) {
      IdlToken name = consumeIdentifier("factory name");
      if (name == null || consume(IdlTokenKind.LEFT_PAREN, "'(' after factory name") == null) {
        synchronizeDeclaration();
        return null;
      }
      List<IdlParameter> parameters = parseFactoryParametersUntilRightParen();
      if (parameters == null) {
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
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after factory declaration");
      if (end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlValueFactory(identifierText(name), parameters, raises, span(start, end));
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
      List<IdlDeclarator> declarators = parseDeclarators();
      if (declarators == null) {
        synchronizeDeclaration();
        return null;
      }
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after attribute declaration");
      if (end == null) {
        synchronizeDeclaration();
        return null;
      }
      return IdlAttribute.withDeclarators(readonly, type, declarators, span(start, end));
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

      List<IdlParameter> parameters = parseParametersUntilRightParen();
      if (parameters == null) {
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
      List<String> contexts = List.of();
      if (matchKeyword("context")) {
        contexts = parseContextClause();
        if (contexts == null) {
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
          oneway, returnType, identifierText(name), parameters, raises, contexts, span(start, end));
    }

    private List<IdlParameter> parseParametersUntilRightParen() {
      List<IdlParameter> parameters = new ArrayList<>();
      if (!check(IdlTokenKind.RIGHT_PAREN)) {
        do {
          IdlParameter parameter = parseParameter();
          if (parameter == null) {
            return null;
          }
          parameters.add(parameter);
        } while (match(IdlTokenKind.COMMA));
      }
      return consume(IdlTokenKind.RIGHT_PAREN, "')' after parameter list") == null
          ? null
          : parameters;
    }

    private List<IdlParameter> parseFactoryParametersUntilRightParen() {
      List<IdlParameter> parameters = new ArrayList<>();
      if (!check(IdlTokenKind.RIGHT_PAREN)) {
        do {
          IdlParameter parameter = parseFactoryParameter();
          if (parameter == null) {
            return null;
          }
          parameters.add(parameter);
        } while (match(IdlTokenKind.COMMA));
      }
      return consume(IdlTokenKind.RIGHT_PAREN, "')' after factory parameter list") == null
          ? null
          : parameters;
    }

    private IdlParameter parseFactoryParameter() {
      IdlToken start = current();
      if (!matchKeyword("in")) {
        emitUnexpected("in factory parameter direction");
        return null;
      }
      IdlTypeReference type = parseType(false);
      IdlToken name = consumeIdentifier("factory parameter name");
      if (type == null || name == null) {
        return null;
      }
      return new IdlParameter(
          IdlParameterDirection.IN, type, identifierText(name), span(start, name));
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

    private List<String> parseContextClause() {
      if (consume(IdlTokenKind.LEFT_PAREN, "'(' after context") == null) {
        return null;
      }
      List<String> contexts = new ArrayList<>();
      do {
        if (!isStringLiteral(current())) {
          emitUnexpected("context string literal");
          return null;
        }
        contexts.add(advance().lexeme());
      } while (match(IdlTokenKind.COMMA));
      if (consume(IdlTokenKind.RIGHT_PAREN, "')' after context clause") == null) {
        return null;
      }
      return contexts;
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
      IdlDeclarator declarator = parseDeclarator();
      if (declarator == null) {
        synchronizeDeclaration();
        return null;
      }
      fields.add(new IdlField(type, declarator, span(type.span(), declarator.span())));
      while (match(IdlTokenKind.COMMA)) {
        declarator = parseDeclarator();
        if (declarator == null) {
          synchronizeDeclaration();
          return null;
        }
        fields.add(new IdlField(type, declarator, span(type.span(), declarator.span())));
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
      IdlConstantExpression expression = parseConstantExpressionUntil(IdlTokenKind.SEMICOLON);
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after constant declaration");
      if (expression == null || end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlConstant(type, identifierText(name), expression, span(start, end));
    }

    private IdlTypedef parseTypedef(IdlToken start) {
      IdlTypeReference type = parseType(false);
      List<IdlDeclarator> declarators = parseDeclarators();
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after typedef declaration");
      if (type == null || declarators == null || end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlTypedef(type, declarators, span(start, end));
    }

    private IdlUnion parseUnion(IdlToken start) {
      IdlToken name = consumeIdentifier("union name");
      if (name == null
          || consumeKeyword("switch", "'switch' after union name") == null
          || consume(IdlTokenKind.LEFT_PAREN, "'(' after switch") == null) {
        synchronizeDeclaration();
        return null;
      }
      IdlTypeReference discriminatorType = parseType(false);
      if (discriminatorType == null
          || consume(IdlTokenKind.RIGHT_PAREN, "')' after union discriminator") == null
          || consume(IdlTokenKind.LEFT_BRACE, "'{' after union discriminator") == null) {
        synchronizeDeclaration();
        return null;
      }
      List<IdlUnionCase> cases = new ArrayList<>();
      while (!check(IdlTokenKind.RIGHT_BRACE) && !isAtEnd()) {
        IdlUnionCase unionCase = parseUnionCase();
        if (unionCase == null) {
          synchronizeDeclaration();
          return null;
        }
        cases.add(unionCase);
      }
      IdlToken close = consume(IdlTokenKind.RIGHT_BRACE, "'}' after union body");
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after union declaration");
      if (cases.isEmpty() || close == null || end == null) {
        synchronizeDeclaration();
        return null;
      }
      return new IdlUnion(identifierText(name), discriminatorType, cases, span(start, end));
    }

    private IdlUnionCase parseUnionCase() {
      IdlToken start = current();
      List<IdlUnionLabel> labels = new ArrayList<>();
      while (checkKeyword("case") || checkKeyword("default")) {
        IdlUnionLabel label = parseUnionLabel();
        if (label == null) {
          return null;
        }
        labels.add(label);
      }
      if (labels.isEmpty()) {
        emitUnexpected("union case label");
        return null;
      }
      IdlTypeReference type = parseType(false);
      IdlDeclarator declarator = parseDeclarator();
      IdlToken end = consume(IdlTokenKind.SEMICOLON, "';' after union member");
      if (type == null || declarator == null || end == null) {
        return null;
      }
      return new IdlUnionCase(labels, type, declarator, span(start, end));
    }

    private IdlUnionLabel parseUnionLabel() {
      IdlToken start = current();
      if (matchKeyword("default")) {
        IdlToken colon = consume(IdlTokenKind.COLON, "':' after default");
        return colon == null ? null : IdlUnionLabel.defaultLabel(span(start, colon));
      }
      if (!matchKeyword("case")) {
        emitUnexpected("case or default");
        return null;
      }
      IdlConstantExpression expression = parseConstantExpressionUntil(IdlTokenKind.COLON);
      IdlToken colon = consume(IdlTokenKind.COLON, "':' after case label");
      if (expression == null || colon == null) {
        return null;
      }
      return IdlUnionLabel.caseLabel(expression, span(start, colon));
    }

    private IdlConstantExpression parseConstantExpressionUntil(IdlTokenKind delimiter) {
      if (expressionDelimiterReached(delimiter)) {
        emitUnexpected("constant expression");
        return null;
      }
      IdlToken start = current();
      IdlToken last = start;
      int parenDepth = 0;
      int bracketDepth = 0;
      int angleDepth = 0;
      List<String> lexemes = new ArrayList<>();
      while (!isAtEnd()) {
        if (parenDepth == 0
            && bracketDepth == 0
            && angleDepth == 0
            && expressionDelimiterReached(delimiter)) {
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
        } else if (match(IdlTokenKind.LESS_THAN)) {
          angleDepth++;
        } else if (match(IdlTokenKind.GREATER_THAN)) {
          angleDepth = Math.max(0, angleDepth - 1);
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
          if (!primitiveType.equals("string") && !primitiveType.equals("wstring")) {
            emitUnsupportedType("bounded type is only valid for string or wstring", current());
            return null;
          }
          return parseBoundedStringType(primitiveType, start);
        }
        return new IdlTypeReference(primitiveType, span(start, end));
      }
      if (matchKeyword("sequence")) {
        return parseSequenceType(start);
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

    private IdlTypeReference parseSequenceType(IdlToken start) {
      if (consume(IdlTokenKind.LESS_THAN, "'<' after sequence") == null) {
        return null;
      }
      IdlTypeReference elementType = parseType(false);
      if (elementType == null) {
        return null;
      }
      IdlConstantExpression bound = null;
      if (match(IdlTokenKind.COMMA)) {
        bound = parseConstantExpressionUntil(IdlTokenKind.GREATER_THAN);
        if (bound == null) {
          return null;
        }
      }
      IdlToken end = consumeGreaterThan("'>' after sequence type");
      if (end == null) {
        return null;
      }
      return bound == null
          ? IdlTypeReference.sequence(elementType, span(start, end))
          : IdlTypeReference.sequence(elementType, bound, span(start, end));
    }

    private IdlTypeReference parseBoundedStringType(String keyword, IdlToken start) {
      if (consume(IdlTokenKind.LESS_THAN, "'<' after " + keyword) == null) {
        return null;
      }
      IdlConstantExpression bound = parseConstantExpressionUntil(IdlTokenKind.GREATER_THAN);
      IdlToken end = consumeGreaterThan("'>' after " + keyword + " bound");
      if (bound == null || end == null) {
        return null;
      }
      return IdlTypeReference.boundedString(keyword, bound, span(start, end));
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

    private List<IdlDeclarator> parseDeclarators() {
      List<IdlDeclarator> declarators = new ArrayList<>();
      IdlDeclarator declarator = parseDeclarator();
      if (declarator == null) {
        return null;
      }
      declarators.add(declarator);
      while (match(IdlTokenKind.COMMA)) {
        declarator = parseDeclarator();
        if (declarator == null) {
          return null;
        }
        declarators.add(declarator);
      }
      return declarators;
    }

    private IdlDeclarator parseDeclarator() {
      IdlToken name = consumeIdentifier("declarator name");
      if (name == null) {
        return null;
      }
      List<IdlArrayDimension> dimensions = new ArrayList<>();
      while (match(IdlTokenKind.LEFT_BRACKET)) {
        IdlToken dimensionStart = previous();
        IdlConstantExpression size = parseConstantExpressionUntil(IdlTokenKind.RIGHT_BRACKET);
        IdlToken end = consume(IdlTokenKind.RIGHT_BRACKET, "']' after array dimension");
        if (size == null || end == null) {
          return null;
        }
        dimensions.add(new IdlArrayDimension(size, span(dimensionStart, end)));
      }
      return new IdlDeclarator(identifierText(name), dimensions, span(name, previous()));
    }

    private IdlToken consumeIdentifier(String label) {
      if (check(IdlTokenKind.IDENTIFIER) || check(IdlTokenKind.ESCAPED_IDENTIFIER)) {
        return advance();
      }
      emitUnexpected(label);
      return null;
    }

    private boolean sameLine(int line) {
      return current().span().start().line() == line;
    }

    private static boolean identifierLike(IdlToken token) {
      return token.kind() == IdlTokenKind.IDENTIFIER
          || token.kind() == IdlTokenKind.ESCAPED_IDENTIFIER
          || token.kind() == IdlTokenKind.KEYWORD;
    }

    private static boolean isStringLiteral(IdlToken token) {
      return token.kind() == IdlTokenKind.STRING_LITERAL
          || token.kind() == IdlTokenKind.WIDE_STRING_LITERAL;
    }

    private static String tokenText(IdlToken token) {
      return token.kind() == IdlTokenKind.ESCAPED_IDENTIFIER
          ? identifierText(token)
          : token.lexeme();
    }

    private IdlToken consume(IdlTokenKind kind, String expectation) {
      if (check(kind)) {
        return advance();
      }
      emitUnexpected(expectation);
      return null;
    }

    private IdlToken consumeGreaterThan(String expectation) {
      if (pendingGreaterThanClosers > 0) {
        pendingGreaterThanClosers--;
        return previous();
      }
      if (check(IdlTokenKind.GREATER_THAN)) {
        return advance();
      }
      if (check(IdlTokenKind.SHIFT_RIGHT)) {
        pendingGreaterThanClosers++;
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
          || checkKeyword("sequence")
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

    private boolean expressionDelimiterReached(IdlTokenKind delimiter) {
      return check(delimiter)
          || (delimiter == IdlTokenKind.GREATER_THAN && check(IdlTokenKind.SHIFT_RIGHT));
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

    private static SourceSpan span(SourceSpan start, SourceSpan end) {
      return new SourceSpan(start.start(), end.end());
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
