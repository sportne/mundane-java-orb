package io.github.mundanej.mjo.notification;

import java.util.List;
import java.util.Locale;

/** Bounded parser and evaluator for the supported local Notification Service filter subset. */
public final class NotificationFilter {

  /** Maximum supported filter expression length. */
  public static final int MAX_EXPRESSION_LENGTH = 512;

  /** Maximum supported nested expression depth. */
  public static final int MAX_DEPTH = 8;

  /** Maximum supported boolean/comparison terms. */
  public static final int MAX_TERMS = 32;

  private final String expression;
  private final Node root;
  private final int maxDepth;
  private final int termCount;

  private NotificationFilter(String expression, Node root, int maxDepth, int termCount) {
    this.expression = expression;
    this.root = root;
    this.maxDepth = maxDepth;
    this.termCount = termCount;
  }

  /** Parses a bounded local Notification Service filter expression. */
  public static NotificationFilter parse(String expression) {
    if (expression == null || expression.isBlank()) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.MALFORMED_FILTER,
          "filter expression must not be blank");
    }
    if (expression.length() > MAX_EXPRESSION_LENGTH) {
      throw new NotificationServiceException(
          NotificationServiceDiagnosticCodes.FILTER_LIMIT_EXCEEDED,
          "filter expression exceeds " + MAX_EXPRESSION_LENGTH + " characters");
    }
    Parser parser = new Parser(expression);
    Node root = parser.parse();
    return new NotificationFilter(expression, root, parser.maxDepth(), parser.termCount());
  }

  /** Returns the original filter expression. */
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

  /** Evaluates this filter against a structured event. */
  public boolean evaluate(NotificationStructuredEvent event) {
    NotificationStructuredEvent checked =
        NotificationStructuredEvent.requirePresent("event", event);
    return root.evaluate(checked);
  }

  private interface Node {
    boolean evaluate(NotificationStructuredEvent event);
  }

  private record ConstantNode(boolean value) implements Node {
    @Override
    public boolean evaluate(NotificationStructuredEvent event) {
      return value;
    }
  }

  private record NotNode(Node child) implements Node {
    @Override
    public boolean evaluate(NotificationStructuredEvent event) {
      return !child.evaluate(event);
    }
  }

  private record BinaryNode(Node left, Node right, boolean conjunction) implements Node {
    @Override
    public boolean evaluate(NotificationStructuredEvent event) {
      boolean leftValue = left.evaluate(event);
      boolean rightValue = right.evaluate(event);
      return conjunction ? leftValue && rightValue : leftValue || rightValue;
    }
  }

  private record ComparisonNode(String field, boolean equality, Literal literal) implements Node {
    @Override
    public boolean evaluate(NotificationStructuredEvent event) {
      Literal actual = resolve(event, field);
      boolean matches = actual.equalsTyped(literal);
      return equality ? matches : !matches;
    }
  }

  private record Literal(NotificationPrimitiveKind kind, Object value) {
    boolean equalsTyped(Literal other) {
      if (kind != other.kind) {
        throw new NotificationServiceException(
            NotificationServiceDiagnosticCodes.FILTER_TYPE_MISMATCH,
            "filter compares " + kind + " with " + other.kind);
      }
      return value.equals(other.value);
    }
  }

  private static Literal resolve(NotificationStructuredEvent event, String field) {
    return switch (field) {
      case "domain_name" ->
          new Literal(NotificationPrimitiveKind.STRING, event.identity().eventType().domainName());
      case "type_name" ->
          new Literal(NotificationPrimitiveKind.STRING, event.identity().eventType().typeName());
      case "event_name" ->
          new Literal(NotificationPrimitiveKind.STRING, event.identity().eventName());
      default -> resolveFilterProperty(event.filterProperties(), field);
    };
  }

  private static Literal resolveFilterProperty(
      List<NotificationProperty> properties, String field) {
    if (!field.startsWith("filter.")) {
      throw unknownField(field);
    }
    String propertyName = field.substring("filter.".length());
    for (NotificationProperty property : properties) {
      if (property.name().equals(propertyName)) {
        NotificationPrimitiveValue value = property.value();
        return switch (value.kind()) {
          case STRING -> new Literal(value.kind(), value.asString());
          case BOOLEAN -> new Literal(value.kind(), Boolean.valueOf(value.asBoolean()));
          case SIGNED_LONG -> new Literal(value.kind(), Long.valueOf(value.asSignedLong()));
          case FLOATING_POINT -> new Literal(value.kind(), Double.valueOf(value.asFloatingPoint()));
        };
      }
    }
    throw unknownField(field);
  }

  private static NotificationServiceException unknownField(String field) {
    return new NotificationServiceException(
        NotificationServiceDiagnosticCodes.UNKNOWN_FILTER_FIELD,
        "filter field is not present in the event: " + field);
  }

  private static final class Parser {
    private final String input;
    private int position;
    private int terms;
    private int maxDepth;

    Parser(String input) {
      this.input = input;
    }

    Node parse() {
      Node node = parseOr(0);
      skipWhitespace();
      if (!atEnd()) {
        throw malformed("unexpected token at position " + position);
      }
      return node;
    }

    int maxDepth() {
      return maxDepth;
    }

    int termCount() {
      return terms;
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
      String field = parseField();
      skipWhitespace();
      boolean equality;
      if (consume("==")) {
        equality = true;
      } else if (consume("!=")) {
        equality = false;
      } else if (peekUnsupportedOperator()) {
        throw new NotificationServiceException(
            NotificationServiceDiagnosticCodes.UNSUPPORTED_FILTER_OPERATOR,
            "filter supports only == and !=");
      } else {
        throw malformed("missing comparison operator");
      }
      Literal literal = parseLiteral();
      bumpTerms();
      return new ComparisonNode(field, equality, literal);
    }

    private String parseField() {
      skipWhitespace();
      String first = parseIdentifier();
      if (consume('.')) {
        return first + "." + parseIdentifier();
      }
      return first;
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
        if (builder.length() > NotificationPrimitiveValue.MAX_STRING_LENGTH) {
          throw new NotificationServiceException(
              NotificationServiceDiagnosticCodes.VALUE_LIMIT_EXCEEDED,
              "filter string literal exceeds "
                  + NotificationPrimitiveValue.MAX_STRING_LENGTH
                  + " characters");
        }
        return new Literal(NotificationPrimitiveKind.STRING, builder.toString());
      }
      if (matchKeyword("true")) {
        return new Literal(NotificationPrimitiveKind.BOOLEAN, true);
      }
      if (matchKeyword("false")) {
        return new Literal(NotificationPrimitiveKind.BOOLEAN, false);
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
      try {
        if (floatingPoint) {
          Double number = Double.valueOf(token);
          if (!Double.isFinite(number.doubleValue())) {
            throw new NotificationServiceException(
                NotificationServiceDiagnosticCodes.UNSUPPORTED_VALUE,
                "floating-point filter literals must be finite");
          }
          return new Literal(NotificationPrimitiveKind.FLOATING_POINT, number);
        }
        return new Literal(NotificationPrimitiveKind.SIGNED_LONG, Long.valueOf(token));
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
      if (identifier.length() > NotificationStructuredEvent.MAX_NAME_LENGTH) {
        throw new NotificationServiceException(
            NotificationServiceDiagnosticCodes.FILTER_LIMIT_EXCEEDED,
            "filter identifier exceeds "
                + NotificationStructuredEvent.MAX_NAME_LENGTH
                + " characters");
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
      return true;
    }

    private boolean peekUnsupportedOperator() {
      return startsWith("=") || startsWith(">") || startsWith("<") || startsWith("!");
    }

    private boolean consume(String token) {
      skipWhitespace();
      if (!startsWith(token)) {
        return false;
      }
      position += token.length();
      return true;
    }

    private boolean consume(char token) {
      skipWhitespace();
      if (atEnd() || input.charAt(position) != token) {
        return false;
      }
      position++;
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
        throw new NotificationServiceException(
            NotificationServiceDiagnosticCodes.FILTER_LIMIT_EXCEEDED,
            "filter expression exceeds depth " + MAX_DEPTH);
      }
      maxDepth = Math.max(maxDepth, depth);
    }

    private void bumpTerms() {
      terms++;
      if (terms > MAX_TERMS) {
        throw new NotificationServiceException(
            NotificationServiceDiagnosticCodes.FILTER_LIMIT_EXCEEDED,
            "filter expression exceeds " + MAX_TERMS + " terms");
      }
    }

    private NotificationServiceException malformed(String message) {
      return new NotificationServiceException(
          NotificationServiceDiagnosticCodes.MALFORMED_FILTER, message);
    }

    private static boolean isIdentifierStart(char value) {
      return Character.isLetter(value) || value == '_';
    }

    private static boolean isIdentifierPart(char value) {
      return Character.isLetterOrDigit(value) || value == '_' || value == '-';
    }
  }
}
