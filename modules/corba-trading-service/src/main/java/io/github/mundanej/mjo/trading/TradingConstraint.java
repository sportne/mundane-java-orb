package io.github.mundanej.mjo.trading;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Bounded parser and evaluator for the supported local Trading Service constraint subset. */
public final class TradingConstraint {

  /** Maximum supported constraint expression length. */
  public static final int MAX_EXPRESSION_LENGTH = 512;

  /** Maximum supported nested expression depth. */
  public static final int MAX_DEPTH = 8;

  /** Maximum supported boolean/comparison terms. */
  public static final int MAX_TERMS = 64;

  /** Maximum supported lexical tokens. */
  public static final int MAX_TOKENS = 128;

  /** Maximum supported string literal length. */
  public static final int MAX_STRING_LITERAL_LENGTH = 4_096;

  private final String expression;
  private final Node root;
  private final int maxDepth;
  private final int termCount;
  private final int tokenCount;

  private TradingConstraint(
      String expression, Node root, int maxDepth, int termCount, int tokenCount) {
    this.expression = expression;
    this.root = root;
    this.maxDepth = maxDepth;
    this.termCount = termCount;
    this.tokenCount = tokenCount;
  }

  /** Parses a bounded local Trading Service constraint expression. */
  public static TradingConstraint parse(String expression) {
    if (expression == null || expression.isBlank()) {
      throw malformed("constraint expression must not be blank");
    }
    if (expression.length() > MAX_EXPRESSION_LENGTH) {
      throw limit("constraint expression exceeds " + MAX_EXPRESSION_LENGTH + " characters");
    }
    Parser parser = new Parser(expression);
    Node root = parser.parse();
    return new TradingConstraint(
        expression, root, parser.maxDepth(), parser.termCount(), parser.tokenCount());
  }

  /** Returns the original constraint expression. */
  public String expression() {
    return expression;
  }

  /** Returns the maximum parsed nested-expression depth. */
  public int maxDepth() {
    return maxDepth;
  }

  /** Returns the parsed boolean/comparison term count. */
  public int termCount() {
    return termCount;
  }

  /** Returns the parsed lexical token count. */
  public int tokenCount() {
    return tokenCount;
  }

  /** Evaluates this constraint against a primitive property map. */
  public boolean evaluate(Map<String, ?> properties) {
    if (properties == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_CONSTRAINT,
          "constraint properties must not be null");
    }
    return root.evaluate(snapshot(properties), null);
  }

  /** Validates this constraint against a registered service type schema. */
  public TradingConstraint validateAgainst(TradingServiceType type) {
    root.validate(schema(type));
    return this;
  }

  /** Evaluates this constraint against a registered service type schema and offer properties. */
  public boolean evaluate(TradingServiceType type, Map<String, ?> properties) {
    if (properties == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_CONSTRAINT,
          "constraint properties must not be null");
    }
    Map<String, TradingPrimitiveKind> schema = schema(type);
    root.validate(schema);
    return root.evaluate(snapshot(properties), schema);
  }

  private interface Node {
    boolean evaluate(Map<String, Object> properties, Map<String, TradingPrimitiveKind> schema);

    void validate(Map<String, TradingPrimitiveKind> schema);
  }

  private record ConstantNode(boolean value) implements Node {
    @Override
    public boolean evaluate(
        Map<String, Object> properties, Map<String, TradingPrimitiveKind> schema) {
      return value;
    }

    @Override
    public void validate(Map<String, TradingPrimitiveKind> schema) {}
  }

  private record NotNode(Node child) implements Node {
    @Override
    public boolean evaluate(
        Map<String, Object> properties, Map<String, TradingPrimitiveKind> schema) {
      return !child.evaluate(properties, schema);
    }

    @Override
    public void validate(Map<String, TradingPrimitiveKind> schema) {
      child.validate(schema);
    }
  }

  private record BinaryNode(Node left, Node right, boolean conjunction) implements Node {
    @Override
    public boolean evaluate(
        Map<String, Object> properties, Map<String, TradingPrimitiveKind> schema) {
      boolean leftValue = left.evaluate(properties, schema);
      boolean rightValue = right.evaluate(properties, schema);
      return conjunction ? leftValue && rightValue : leftValue || rightValue;
    }

    @Override
    public void validate(Map<String, TradingPrimitiveKind> schema) {
      left.validate(schema);
      right.validate(schema);
    }
  }

  private record ComparisonNode(String propertyName, Operator operator, Literal literal)
      implements Node {
    @Override
    public boolean evaluate(
        Map<String, Object> properties, Map<String, TradingPrimitiveKind> schema) {
      Literal actual = resolve(properties, propertyName, schema);
      return actual.compare(operator, literal);
    }

    @Override
    public void validate(Map<String, TradingPrimitiveKind> schema) {
      TradingPrimitiveKind propertyKind = schema.get(propertyName);
      if (propertyKind == null) {
        throw new TradingServiceException(
            TradingServiceDiagnosticCodes.UNKNOWN_CONSTRAINT_PROPERTY,
            "constraint property is not defined by service type: " + propertyName);
      }
      if (propertyKind != literal.kind()) {
        throw new TradingServiceException(
            TradingServiceDiagnosticCodes.CONSTRAINT_TYPE_MISMATCH,
            "constraint compares " + propertyKind + " with " + literal.kind());
      }
      if (!isOrderedKind(propertyKind)
          && (operator == Operator.GREATER_THAN
              || operator == Operator.GREATER_THAN_OR_EQUAL
              || operator == Operator.LESS_THAN
              || operator == Operator.LESS_THAN_OR_EQUAL)) {
        throw new TradingServiceException(
            TradingServiceDiagnosticCodes.CONSTRAINT_TYPE_MISMATCH,
            "ordered constraint comparisons require numeric values");
      }
    }
  }

  private enum Operator {
    EQUAL,
    NOT_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL
  }

  private record Literal(TradingPrimitiveKind kind, Object value) {
    boolean compare(Operator operator, Literal other) {
      if (kind != other.kind) {
        throw new TradingServiceException(
            TradingServiceDiagnosticCodes.CONSTRAINT_TYPE_MISMATCH,
            "constraint compares " + kind + " with " + other.kind);
      }
      if (value == null) {
        return false;
      }
      return switch (operator) {
        case EQUAL -> value.equals(other.value);
        case NOT_EQUAL -> !value.equals(other.value);
        case GREATER_THAN -> compareOrdered(other) > 0;
        case GREATER_THAN_OR_EQUAL -> compareOrdered(other) >= 0;
        case LESS_THAN -> compareOrdered(other) < 0;
        case LESS_THAN_OR_EQUAL -> compareOrdered(other) <= 0;
      };
    }

    private int compareOrdered(Literal other) {
      return switch (kind) {
        case SIGNED_LONG -> Long.compare((Long) value, (Long) other.value);
        case FLOATING_POINT -> Double.compare((Double) value, (Double) other.value);
        case STRING, BOOLEAN ->
            throw new TradingServiceException(
                TradingServiceDiagnosticCodes.CONSTRAINT_TYPE_MISMATCH,
                "ordered constraint comparisons require numeric values");
      };
    }
  }

  private static Literal resolve(
      Map<String, Object> properties,
      String propertyName,
      Map<String, TradingPrimitiveKind> schema) {
    if (!properties.containsKey(propertyName)) {
      if (schema != null && schema.containsKey(propertyName)) {
        return new Literal(schema.get(propertyName), null);
      }
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.UNKNOWN_CONSTRAINT_PROPERTY,
          "constraint property is not present: " + propertyName);
    }
    Object value = properties.get(propertyName);
    return literalFromValue(propertyName, value);
  }

  private static Literal literalFromValue(String label, Object value) {
    if (value instanceof String text) {
      return new Literal(TradingPrimitiveKind.STRING, text);
    }
    if (value instanceof Boolean bool) {
      return new Literal(TradingPrimitiveKind.BOOLEAN, bool);
    }
    if (value instanceof Long number) {
      return new Literal(TradingPrimitiveKind.SIGNED_LONG, number);
    }
    if (value instanceof Double number && Double.isFinite(number)) {
      return new Literal(TradingPrimitiveKind.FLOATING_POINT, number);
    }
    throw new TradingServiceException(
        TradingServiceDiagnosticCodes.UNSUPPORTED_VALUE,
        "unsupported constraint property value: " + label);
  }

  private static Map<String, Object> snapshot(Map<String, ?> properties) {
    Map<String, Object> copy = new LinkedHashMap<>();
    for (Map.Entry<String, ?> entry : properties.entrySet()) {
      String propertyName =
          TradingNames.requireName(
              entry.getKey(), "constraint property name", TradingServiceOptions.modelLimits());
      copy.put(propertyName, entry.getValue());
    }
    return Collections.unmodifiableMap(copy);
  }

  private static Map<String, TradingPrimitiveKind> schema(TradingServiceType type) {
    if (type == null) {
      throw new TradingServiceException(
          TradingServiceDiagnosticCodes.MALFORMED_CONSTRAINT,
          "constraint service type must not be null");
    }
    Map<String, TradingPrimitiveKind> schema = new LinkedHashMap<>();
    for (TradingPropertyDefinition definition : type.properties()) {
      schema.put(definition.name(), definition.kind());
    }
    return Collections.unmodifiableMap(schema);
  }

  private static boolean isOrderedKind(TradingPrimitiveKind kind) {
    return kind == TradingPrimitiveKind.SIGNED_LONG || kind == TradingPrimitiveKind.FLOATING_POINT;
  }

  private static TradingServiceException malformed(String message) {
    return new TradingServiceException(TradingServiceDiagnosticCodes.MALFORMED_CONSTRAINT, message);
  }

  private static TradingServiceException limit(String message) {
    return new TradingServiceException(
        TradingServiceDiagnosticCodes.CONSTRAINT_LIMIT_EXCEEDED, message);
  }

  private static TradingServiceException unsupported(String message) {
    return new TradingServiceException(
        TradingServiceDiagnosticCodes.UNSUPPORTED_CONSTRAINT_OPERATOR, message);
  }

  private static final class Parser {
    private final String input;
    private int position;
    private int tokens;
    private int terms;
    private int maxDepth;

    Parser(String input) {
      this.input = input;
    }

    Node parse() {
      Node node = parseOr(0);
      skipWhitespace();
      if (!atEnd()) {
        throw unsupportedOrMalformed();
      }
      return node;
    }

    int maxDepth() {
      return maxDepth;
    }

    int termCount() {
      return terms;
    }

    int tokenCount() {
      return tokens;
    }

    private Node parseOr(int depth) {
      Node node = parseAnd(depth);
      while (matchKeyword("or")) {
        node = new BinaryNode(node, parseAnd(depth), false);
      }
      return node;
    }

    private Node parseAnd(int depth) {
      Node node = parseUnary(depth);
      while (matchKeyword("and")) {
        node = new BinaryNode(node, parseUnary(depth), true);
      }
      return node;
    }

    private Node parseUnary(int depth) {
      checkDepth(depth);
      if (matchKeyword("not")) {
        return new NotNode(parseUnary(depth + 1));
      }
      return parsePrimary(depth);
    }

    private Node parsePrimary(int depth) {
      checkDepth(depth);
      skipWhitespace();
      if (consume('(')) {
        Node node = parseOr(depth + 1);
        if (!consume(')')) {
          throw malformed("missing closing parenthesis");
        }
        return node;
      }
      if (matchKeyword("true")) {
        bumpTerms();
        return new ConstantNode(true);
      }
      if (matchKeyword("false")) {
        bumpTerms();
        return new ConstantNode(false);
      }
      return parseComparison();
    }

    private Node parseComparison() {
      String propertyName = parsePropertyName();
      Operator operator = parseOperator();
      Literal literal = parseLiteral();
      bumpTerms();
      return new ComparisonNode(propertyName, operator, literal);
    }

    private Operator parseOperator() {
      skipWhitespace();
      if (consume("==")) {
        return Operator.EQUAL;
      }
      if (consume("!=")) {
        return Operator.NOT_EQUAL;
      }
      if (consume(">=")) {
        return Operator.GREATER_THAN_OR_EQUAL;
      }
      if (consume(">")) {
        return Operator.GREATER_THAN;
      }
      if (consume("<=")) {
        return Operator.LESS_THAN_OR_EQUAL;
      }
      if (consume("<")) {
        return Operator.LESS_THAN;
      }
      if (peekUnsupportedOperator()) {
        throw unsupported("unsupported constraint operator at position " + position);
      }
      throw malformed("missing comparison operator");
    }

    private String parsePropertyName() {
      String propertyName = parseIdentifier();
      skipWhitespace();
      if (!atEnd() && input.charAt(position) == '(') {
        throw unsupported("constraint functions are unsupported: " + propertyName);
      }
      return propertyName;
    }

    private Literal parseLiteral() {
      skipWhitespace();
      if (consume('\'')) {
        StringBuilder builder = new StringBuilder();
        while (!atEnd() && input.charAt(position) != '\'') {
          char current = input.charAt(position++);
          if (current == '\\') {
            if (atEnd()) {
              throw malformed("unterminated string escape");
            }
            current = input.charAt(position++);
          }
          builder.append(current);
        }
        if (!consume('\'')) {
          throw malformed("unterminated string literal");
        }
        if (builder.length() > MAX_STRING_LITERAL_LENGTH) {
          throw limit(
              "constraint string literal exceeds " + MAX_STRING_LITERAL_LENGTH + " characters");
        }
        return new Literal(TradingPrimitiveKind.STRING, builder.toString());
      }
      if (matchKeyword("true")) {
        return new Literal(TradingPrimitiveKind.BOOLEAN, true);
      }
      if (matchKeyword("false")) {
        return new Literal(TradingPrimitiveKind.BOOLEAN, false);
      }
      if (!atEnd() && isUnsupportedLiteralStart(input.charAt(position))) {
        throw unsupported("unsupported constraint literal at position " + position);
      }
      return parseNumber();
    }

    private Literal parseNumber() {
      skipWhitespace();
      int start = position;
      if (!atEnd() && input.charAt(position) == '-') {
        position++;
      }
      while (!atEnd() && Character.isDigit(input.charAt(position))) {
        position++;
      }
      boolean floatingPoint = false;
      if (!atEnd() && input.charAt(position) == '.') {
        floatingPoint = true;
        position++;
        while (!atEnd() && Character.isDigit(input.charAt(position))) {
          position++;
        }
      }
      if (start == position || (input.charAt(start) == '-' && start + 1 == position)) {
        throw malformed("missing literal");
      }
      String token = input.substring(start, position);
      bumpTokens();
      try {
        if (floatingPoint) {
          Double number = Double.valueOf(token);
          if (!Double.isFinite(number)) {
            throw new TradingServiceException(
                TradingServiceDiagnosticCodes.UNSUPPORTED_VALUE,
                "floating-point constraint literals must be finite");
          }
          return new Literal(TradingPrimitiveKind.FLOATING_POINT, number);
        }
        return new Literal(TradingPrimitiveKind.SIGNED_LONG, Long.valueOf(token));
      } catch (NumberFormatException ex) {
        throw malformed("invalid numeric literal");
      }
    }

    private String parseIdentifier() {
      skipWhitespace();
      int start = position;
      if (atEnd() || !isIdentifierStart(input.charAt(position))) {
        throw malformed("missing identifier");
      }
      position++;
      while (!atEnd() && isIdentifierPart(input.charAt(position))) {
        position++;
      }
      String identifier = input.substring(start, position);
      if (identifier.length() > TradingServiceOptions.MAX_SUPPORTED_LIMIT) {
        throw limit(
            "constraint identifier exceeds "
                + TradingServiceOptions.MAX_SUPPORTED_LIMIT
                + " characters");
      }
      bumpTokens();
      if (isReservedKeyword(identifier)) {
        throw malformed("reserved keyword is not a property name: " + identifier);
      }
      return identifier;
    }

    private boolean matchKeyword(String keyword) {
      skipWhitespace();
      int end = position + keyword.length();
      if (end > input.length()) {
        return false;
      }
      String candidate = input.substring(position, end).toLowerCase(Locale.ROOT);
      if (!candidate.equals(keyword)
          || (end < input.length() && isIdentifierPart(input.charAt(end)))) {
        return false;
      }
      position = end;
      bumpTokens();
      return true;
    }

    private boolean consume(String token) {
      skipWhitespace();
      if (!startsWith(token)) {
        return false;
      }
      position += token.length();
      bumpTokens();
      return true;
    }

    private boolean consume(char token) {
      skipWhitespace();
      if (atEnd() || input.charAt(position) != token) {
        return false;
      }
      position++;
      bumpTokens();
      return true;
    }

    private boolean startsWith(String token) {
      return input.startsWith(token, position);
    }

    private void skipWhitespace() {
      while (!atEnd() && Character.isWhitespace(input.charAt(position))) {
        position++;
      }
    }

    private boolean atEnd() {
      return position >= input.length();
    }

    private void checkDepth(int depth) {
      if (depth > MAX_DEPTH) {
        throw limit("constraint expression exceeds depth " + MAX_DEPTH);
      }
      maxDepth = Math.max(maxDepth, depth);
    }

    private void bumpTerms() {
      terms++;
      if (terms > MAX_TERMS) {
        throw limit("constraint expression exceeds " + MAX_TERMS + " terms");
      }
    }

    private void bumpTokens() {
      tokens++;
      if (tokens > MAX_TOKENS) {
        throw limit("constraint expression exceeds " + MAX_TOKENS + " tokens");
      }
    }

    private boolean peekUnsupportedOperator() {
      return startsWith("=")
          || startsWith("!")
          || startsWith(">")
          || startsWith("<")
          || startsWith("+")
          || startsWith("-")
          || startsWith("*")
          || startsWith("/")
          || startsWith("%");
    }

    private TradingServiceException unsupportedOrMalformed() {
      char current = input.charAt(position);
      if (current == '*'
          || current == '/'
          || current == '%'
          || current == '+'
          || current == '~'
          || current == '['
          || current == ']'
          || current == '{'
          || current == '}') {
        return unsupported("unsupported constraint syntax at position " + position);
      }
      return malformed("unexpected token at position " + position);
    }

    private static boolean isIdentifierStart(char value) {
      return Character.isLetter(value) || value == '_';
    }

    private static boolean isIdentifierPart(char value) {
      return Character.isLetterOrDigit(value) || value == '_' || value == '-';
    }

    private static boolean isReservedKeyword(String identifier) {
      String lower = identifier.toLowerCase(Locale.ROOT);
      return lower.equals("and")
          || lower.equals("or")
          || lower.equals("not")
          || lower.equals("true")
          || lower.equals("false");
    }

    private static boolean isUnsupportedLiteralStart(char value) {
      return value == '*'
          || value == '/'
          || value == '~'
          || value == '['
          || value == ']'
          || value == '{'
          || value == '}';
    }
  }
}
