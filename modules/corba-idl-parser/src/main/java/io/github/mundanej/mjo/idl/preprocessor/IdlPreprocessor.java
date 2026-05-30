package io.github.mundanej.mjo.idl.preprocessor;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import io.github.mundanej.mjo.common.LimitViolation;
import io.github.mundanej.mjo.common.SourcePosition;
import io.github.mundanej.mjo.common.SourceSpan;
import io.github.mundanej.mjo.idl.lexer.IdlDiagnosticCodes;
import io.github.mundanej.mjo.idl.lexer.IdlLexResult;
import io.github.mundanej.mjo.idl.lexer.IdlLexer;
import io.github.mundanej.mjo.idl.lexer.IdlToken;
import io.github.mundanej.mjo.idl.lexer.IdlTokenKind;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Minimal C-style preprocessor for IDL translation units. */
public final class IdlPreprocessor {

  private static final IdlIncludeResolver EMPTY_INCLUDE_RESOLVER = request -> Optional.empty();

  private final IdlLexer lexer;
  private final IdlIncludeResolver includeResolver;

  /** Creates a preprocessor with no external include resolver. */
  public IdlPreprocessor() {
    this(EMPTY_INCLUDE_RESOLVER);
  }

  /** Creates a preprocessor with a caller-supplied include resolver. */
  public IdlPreprocessor(IdlIncludeResolver includeResolver) {
    this(new IdlLexer(), includeResolver);
  }

  IdlPreprocessor(IdlLexer lexer, IdlIncludeResolver includeResolver) {
    this.lexer = Objects.requireNonNull(lexer, "lexer");
    this.includeResolver = Objects.requireNonNull(includeResolver, "includeResolver");
  }

  /** Preprocesses source using default bounded preprocessing options. */
  public IdlPreprocessResult preprocess(IdlSource source) {
    return preprocess(source, IdlPreprocessorOptions.defaults());
  }

  /** Preprocesses source using caller-supplied bounded preprocessing options. */
  public IdlPreprocessResult preprocess(IdlSource source, IdlPreprocessorOptions options) {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(options, "options");
    return new Engine(lexer, includeResolver, options).preprocess(source);
  }

  private static final class Engine {

    private final IdlLexer lexer;
    private final IdlIncludeResolver includeResolver;
    private final IdlPreprocessorOptions options;
    private final Map<String, Macro> macros = new HashMap<>();
    private final List<IdlToken> outputTokens = new ArrayList<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final List<String> includedSourceNames = new ArrayList<>();
    private long macroExpansionCount;
    private boolean diagnosticLimitReached;

    private Engine(
        IdlLexer lexer, IdlIncludeResolver includeResolver, IdlPreprocessorOptions options) {
      this.lexer = lexer;
      this.includeResolver = includeResolver;
      this.options = options;
    }

    private IdlPreprocessResult preprocess(IdlSource source) {
      NormalizedSource normalized = processSource(source, new ArrayDeque<>(), 0);
      SourceSpan eofSpan = new SourceSpan(normalized.eofPosition(), normalized.eofPosition());
      outputTokens.add(new IdlToken(IdlTokenKind.END_OF_FILE, "", eofSpan));
      return new IdlPreprocessResult(outputTokens, diagnostics, includedSourceNames);
    }

    private NormalizedSource processSource(
        IdlSource source, ArrayDeque<String> includeStack, int includeDepth) {
      NormalizedSource normalized = NormalizedSource.from(source);
      includeStack.addLast(source.sourceName());
      SourceView sourceView = lexSource(normalized);
      ConditionalState conditionals = new ConditionalState();
      ActiveLineMarkers lineMarkers = new ActiveLineMarkers(source.sourceName());

      for (LineView line : sourceView.lines()) {
        LineView emittedLine = line.remap(lineMarkers);
        if (line.isDirective()) {
          processDirective(
              line, emittedLine, conditionals, includeStack, includeDepth, lineMarkers);
        } else if (conditionals.isActive()) {
          outputTokens.addAll(expandTokens(emittedLine.emittedTokens(), new HashSet<>()));
          lexerDiagnosticsFor(emittedLine).forEach(this::addDiagnostic);
        }
        lineMarkers.finishLine();
      }

      for (ConditionalFrame frame : conditionals.openFrames()) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.UNTERMINATED_CONDITIONAL,
            "Unterminated conditional directive",
            frame.startSpan());
      }
      includeStack.removeLast();
      return normalized;
    }

    private SourceView lexSource(NormalizedSource normalized) {
      IdlLexResult lexResult =
          lexer.tokenize(normalized.sourceName(), normalized.text(), options.lexerOptions());
      Set<Integer> directiveLines = normalized.directiveLines();
      TreeMap<Integer, List<Diagnostic>> diagnosticsByLine = new TreeMap<>();
      for (Diagnostic diagnostic : lexResult.diagnostics()) {
        Optional<SourceSpan> span = diagnostic.span();
        if (span.isPresent() && directiveLines.contains(span.orElseThrow().start().line())) {
          continue;
        }
        Diagnostic remapped = normalized.remap(diagnostic);
        int line = span.map(value -> value.start().line()).orElse(1);
        diagnosticsByLine.computeIfAbsent(line, ignored -> new ArrayList<>()).add(remapped);
      }

      TreeMap<Integer, List<TokenEntry>> grouped = new TreeMap<>();
      for (IdlToken token : lexResult.tokens()) {
        if (token.kind() == IdlTokenKind.END_OF_FILE) {
          continue;
        }
        int line = token.span().start().line();
        grouped
            .computeIfAbsent(line, ignored -> new ArrayList<>())
            .add(new TokenEntry(token, normalized.remap(token), line));
      }

      TreeSet<Integer> lineNumbers = new TreeSet<>();
      lineNumbers.addAll(grouped.keySet());
      lineNumbers.addAll(diagnosticsByLine.keySet());
      List<LineView> lines = new ArrayList<>();
      for (Integer lineNumber : lineNumbers) {
        lines.add(
            new LineView(
                lineNumber,
                normalized.lineText(lineNumber),
                grouped.getOrDefault(lineNumber, List.of()),
                diagnosticsByLine.getOrDefault(lineNumber, List.of())));
      }
      return new SourceView(lines);
    }

    private List<Diagnostic> lexerDiagnosticsFor(LineView line) {
      List<SourceSpan> macroSpans =
          line.emittedTokens().stream()
              .filter(token -> macroName(token).map(macros::containsKey).orElse(false))
              .map(IdlToken::span)
              .toList();
      return line.lexerDiagnostics().stream()
          .filter(
              diagnostic ->
                  !IdlDiagnosticCodes.KEYWORD_CASE_COLLISION.equals(diagnostic.code())
                      || diagnostic.span().stream().noneMatch(macroSpans::contains))
          .toList();
    }

    private void processDirective(
        LineView lexedLine,
        LineView emittedLine,
        ConditionalState conditionals,
        ArrayDeque<String> includeStack,
        int includeDepth,
        ActiveLineMarkers lineMarkers) {
      if (LineMarkerDirective.looksLikeLineMarker(lexedLine)) {
        Optional<LineMarkerDirective> lineMarker = LineMarkerDirective.parse(lexedLine.lineText());
        if (conditionals.isActive() && lineMarker.isEmpty()) {
          emitDiagnostic(
              IdlPreprocessorDiagnosticCodes.MALFORMED_LINE_MARKER,
              "Malformed IDL line marker directive",
              emittedLine.directiveSpan());
        } else if (conditionals.isActive()) {
          lineMarkers.apply(lineMarker.orElseThrow());
        }
        return;
      }

      String directive = lexedLine.directiveName().orElse("");
      switch (directive) {
        case "include" -> {
          if (conditionals.isActive()) {
            processInclude(emittedLine, includeStack, includeDepth);
          }
        }
        case "define" -> {
          if (conditionals.isActive()) {
            processDefine(emittedLine);
          }
        }
        case "undef" -> {
          if (conditionals.isActive()) {
            processUndef(emittedLine);
          }
        }
        case "ifdef" -> processIfdef(emittedLine, conditionals, false);
        case "ifndef" -> processIfdef(emittedLine, conditionals, true);
        case "if" -> processIf(emittedLine, conditionals);
        case "elif" -> processElif(emittedLine, conditionals);
        case "else" -> processElse(emittedLine, conditionals);
        case "endif" -> processEndif(emittedLine, conditionals);
        case "pragma" -> {
          if (conditionals.isActive()) {
            outputTokens.addAll(emittedLine.emittedTokens());
          }
        }
        default -> {
          if (conditionals.isActive()) {
            emitDiagnostic(
                IdlPreprocessorDiagnosticCodes.UNSUPPORTED_DIRECTIVE,
                "Unsupported IDL preprocessor directive: " + directive,
                emittedLine.directiveSpan());
          }
        }
      }
    }

    private void processInclude(LineView line, ArrayDeque<String> includeStack, int includeDepth) {
      Optional<IncludeDirective> parsed = IncludeDirective.parse(line);
      if (parsed.isEmpty()) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.MALFORMED_INCLUDE,
            "Malformed IDL include directive",
            line.directiveSpan());
        return;
      }

      IncludeDirective include = parsed.orElseThrow();
      options
          .includeDepthLimit()
          .check(includeDepth + 1L)
          .ifPresent(
              violation ->
                  emitDiagnostic(
                      IdlPreprocessorDiagnosticCodes.INCLUDE_DEPTH_EXCEEDED,
                      violation.message(),
                      line.directiveSpan()));
      if (!options.includeDepthLimit().accepts(includeDepth + 1L)) {
        return;
      }

      IdlIncludeRequest request =
          new IdlIncludeRequest(
              include.includeName(),
              include.kind(),
              line.requestingSourceName(),
              line.directiveSpan());
      IdlSource included;
      try {
        Optional<IdlSource> resolved = includeResolver.resolve(request);
        if (resolved.isEmpty()) {
          emitDiagnostic(
              IdlPreprocessorDiagnosticCodes.INCLUDE_NOT_FOUND,
              "IDL include could not be resolved: " + include.includeName(),
              line.directiveSpan());
          return;
        }
        included = resolved.orElseThrow();
      } catch (SecurityException | IllegalArgumentException exception) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.UNSAFE_INCLUDE_PATH,
            exception.getMessage(),
            line.directiveSpan());
        return;
      } catch (IOException exception) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.INCLUDE_NOT_FOUND,
            "IDL include could not be read: " + include.includeName(),
            line.directiveSpan());
        return;
      }

      if (includeStack.contains(included.sourceName())) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.INCLUDE_CYCLE,
            "IDL include cycle detected for source: " + included.sourceName(),
            line.directiveSpan());
        return;
      }
      includedSourceNames.add(included.sourceName());
      processSource(included, includeStack, includeDepth + 1);
    }

    private void processDefine(LineView line) {
      List<TokenEntry> tokens = line.tokens();
      if (tokens.size() < 3 || !isMacroNameToken(tokens.get(2).emitted())) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.MALFORMED_MACRO,
            "Malformed IDL macro definition",
            line.directiveSpan());
        return;
      }

      String name = macroName(tokens.get(2).emitted()).orElseThrow();

      int bodyStart = 3;
      List<String> parameters = List.of();
      boolean functionLike = false;
      if (tokens.size() > 3 && tokens.get(3).emitted().kind() == IdlTokenKind.LEFT_PAREN) {
        if (tokens.get(2).lexed().span().end().offset() + 1
            == tokens.get(3).lexed().span().start().offset()) {
          functionLike = true;
          if (hasVariadicParameterMarker(line, name)) {
            emitDiagnostic(
                IdlPreprocessorDiagnosticCodes.UNSUPPORTED_MACRO_OPERATOR,
                "Variadic IDL macros are not supported in this slice",
                line.directiveSpan());
            return;
          }
          ParsedParameters parsed = parseParameters(tokens, 4, line.directiveSpan());
          if (!parsed.valid()) {
            return;
          }
          parameters = parsed.parameters();
          bodyStart = parsed.bodyStart();
        }
      }

      List<IdlToken> replacement =
          tokens.subList(bodyStart, tokens.size()).stream().map(TokenEntry::emitted).toList();
      if (replacement.stream().anyMatch(Engine::isVariadicReplacementToken)) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.UNSUPPORTED_MACRO_OPERATOR,
            "Variadic IDL macros are not supported in this slice",
            line.directiveSpan());
        return;
      }
      if (replacement.stream()
          .anyMatch(
              token ->
                  token.kind() == IdlTokenKind.HASH || token.kind() == IdlTokenKind.DOUBLE_HASH)) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.UNSUPPORTED_MACRO_OPERATOR,
            "Macro stringification and token pasting are deferred from this slice",
            line.directiveSpan());
        return;
      }

      if (macros.containsKey(name)) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.MACRO_REDEFINED,
            "IDL macro redefined: " + name,
            tokens.get(2).emitted().span());
      }
      macros.put(name, new Macro(name, parameters, replacement, functionLike));
    }

    private static boolean hasVariadicParameterMarker(LineView line, String name) {
      String text = line.lineText();
      int directiveStart = skipWhitespace(text, 0);
      if (directiveStart >= text.length() || text.charAt(directiveStart) != '#') {
        return false;
      }
      int index = skipWhitespace(text, directiveStart + 1);
      index += "define".length();
      index = skipWhitespace(text, index);
      if (!text.startsWith(name, index)) {
        return false;
      }
      index += name.length();
      if (index >= text.length() || text.charAt(index) != '(') {
        return false;
      }
      int close = text.indexOf(')', index + 1);
      if (close < 0) {
        return false;
      }
      String parameterText = text.substring(index + 1, close);
      return parameterText.contains("...") || parameterText.contains("__VA_ARGS__");
    }

    private static boolean isVariadicReplacementToken(IdlToken token) {
      return macroName(token).map("__VA_ARGS__"::equals).orElse(false);
    }

    private ParsedParameters parseParameters(
        List<TokenEntry> tokens, int index, SourceSpan directiveSpan) {
      List<String> parameters = new ArrayList<>();
      boolean expectName = true;
      int current = index;
      while (current < tokens.size()) {
        IdlToken token = tokens.get(current).emitted();
        if (token.kind() == IdlTokenKind.RIGHT_PAREN) {
          return new ParsedParameters(true, List.copyOf(parameters), current + 1);
        }
        if (expectName) {
          Optional<String> parameterName = macroName(token);
          if (parameterName.isEmpty()) {
            emitDiagnostic(
                IdlPreprocessorDiagnosticCodes.MALFORMED_MACRO,
                "Malformed function-like IDL macro parameter list",
                directiveSpan);
            return ParsedParameters.invalid();
          }
          parameters.add(parameterName.orElseThrow());
          expectName = false;
        } else if (token.kind() == IdlTokenKind.COMMA) {
          expectName = true;
        } else {
          emitDiagnostic(
              IdlPreprocessorDiagnosticCodes.MALFORMED_MACRO,
              "Malformed function-like IDL macro parameter list",
              directiveSpan);
          return ParsedParameters.invalid();
        }
        current++;
      }
      emitDiagnostic(
          IdlPreprocessorDiagnosticCodes.MALFORMED_MACRO,
          "Unterminated function-like IDL macro parameter list",
          directiveSpan);
      return ParsedParameters.invalid();
    }

    private void processUndef(LineView line) {
      if (line.tokens().size() < 3 || !isMacroNameToken(line.tokens().get(2).emitted())) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.MALFORMED_MACRO,
            "Malformed IDL macro undef directive",
            line.directiveSpan());
        return;
      }
      macros.remove(macroName(line.tokens().get(2).emitted()).orElseThrow());
    }

    private void processIfdef(LineView line, ConditionalState conditionals, boolean negate) {
      if (!conditionals.isActive()) {
        conditionals.push(false, line.directiveSpan());
        return;
      }
      Optional<String> name =
          line.tokens().size() >= 3 ? macroName(line.tokens().get(2).emitted()) : Optional.empty();
      if (name.isEmpty()) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.MALFORMED_CONDITIONAL,
            "Malformed IDL conditional directive",
            line.directiveSpan());
      }
      boolean selected = name.map(macros::containsKey).orElse(false);
      if (negate) {
        selected = !selected;
      }
      conditionals.push(selected, line.directiveSpan());
    }

    private void processIf(LineView line, ConditionalState conditionals) {
      if (!conditionals.isActive()) {
        conditionals.push(false, line.directiveSpan());
        return;
      }
      ConditionalExpression expression =
          new ConditionalExpression(line.afterDirectiveTokens(), macros);
      ConditionalExpressionResult result = expression.evaluate();
      result
          .diagnostic()
          .ifPresent(
              message ->
                  emitDiagnostic(
                      IdlPreprocessorDiagnosticCodes.UNSUPPORTED_CONDITIONAL_EXPRESSION,
                      message,
                      line.directiveSpan()));
      conditionals.push(result.value(), line.directiveSpan());
    }

    private void processElif(LineView line, ConditionalState conditionals) {
      if (!conditionals.hasOpenFrame()) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.MALFORMED_CONDITIONAL,
            "Unmatched IDL #elif directive",
            line.directiveSpan());
        return;
      }
      boolean selected = false;
      if (conditionals.shouldEvaluateElif()) {
        ConditionalExpression expression =
            new ConditionalExpression(line.afterDirectiveTokens(), macros);
        ConditionalExpressionResult result = expression.evaluate();
        result
            .diagnostic()
            .ifPresent(
                message ->
                    emitDiagnostic(
                        IdlPreprocessorDiagnosticCodes.UNSUPPORTED_CONDITIONAL_EXPRESSION,
                        message,
                        line.directiveSpan()));
        selected = result.value();
      }
      conditionals
          .elif(selected)
          .ifPresent(
              message ->
                  emitDiagnostic(
                      IdlPreprocessorDiagnosticCodes.MALFORMED_CONDITIONAL,
                      message,
                      line.directiveSpan()));
    }

    private void processElse(LineView line, ConditionalState conditionals) {
      if (!conditionals.hasOpenFrame()) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.MALFORMED_CONDITIONAL,
            "Unmatched IDL #else directive",
            line.directiveSpan());
        return;
      }
      conditionals
          .elseBranch()
          .ifPresent(
              message ->
                  emitDiagnostic(
                      IdlPreprocessorDiagnosticCodes.MALFORMED_CONDITIONAL,
                      message,
                      line.directiveSpan()));
    }

    private void processEndif(LineView line, ConditionalState conditionals) {
      if (!conditionals.hasOpenFrame()) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.MALFORMED_CONDITIONAL,
            "Unmatched IDL #endif directive",
            line.directiveSpan());
        return;
      }
      conditionals.pop();
    }

    private List<IdlToken> expandTokens(List<IdlToken> tokens, Set<String> expansionStack) {
      List<IdlToken> expanded = new ArrayList<>();
      int index = 0;
      while (index < tokens.size()) {
        IdlToken token = tokens.get(index);
        Optional<String> maybeName = macroName(token);
        if (maybeName.isEmpty() || !macros.containsKey(maybeName.orElseThrow())) {
          expanded.add(token);
          index++;
          continue;
        }

        String name = maybeName.orElseThrow();
        Macro macro = macros.get(name);
        if (macro.functionLike()) {
          if (index + 1 >= tokens.size()
              || tokens.get(index + 1).kind() != IdlTokenKind.LEFT_PAREN) {
            expanded.add(token);
            index++;
            continue;
          }
          ParsedArguments arguments = parseArguments(tokens, index + 2, token.span());
          if (!arguments.valid() || arguments.arguments().size() != macro.parameters().size()) {
            emitDiagnostic(
                IdlPreprocessorDiagnosticCodes.MALFORMED_MACRO,
                "Malformed invocation for IDL macro: " + name,
                token.span());
            expanded.add(token);
            index++;
            continue;
          }
          if (!startExpansion(name, token.span(), expansionStack)) {
            expanded.add(token);
            index++;
            continue;
          }
          expanded.addAll(
              expandTokens(macro.expand(arguments.arguments(), token.span()), expansionStack));
          expansionStack.remove(name);
          index = arguments.endIndex() + 1;
        } else {
          if (!startExpansion(name, token.span(), expansionStack)) {
            expanded.add(token);
            index++;
            continue;
          }
          expanded.addAll(expandTokens(macro.expand(List.of(), token.span()), expansionStack));
          expansionStack.remove(name);
          index++;
        }
      }
      return expanded;
    }

    private boolean startExpansion(String name, SourceSpan span, Set<String> expansionStack) {
      if (expansionStack.contains(name)) {
        emitDiagnostic(
            IdlPreprocessorDiagnosticCodes.RECURSIVE_MACRO,
            "Recursive IDL macro expansion rejected: " + name,
            span);
        return false;
      }
      long nextExpansionCount = macroExpansionCount + 1L;
      options
          .macroExpansionLimit()
          .check(nextExpansionCount)
          .ifPresent(
              violation ->
                  emitDiagnostic(
                      IdlPreprocessorDiagnosticCodes.MACRO_EXPANSION_LIMIT_EXCEEDED,
                      violation.message(),
                      span));
      if (!options.macroExpansionLimit().accepts(nextExpansionCount)) {
        return false;
      }
      macroExpansionCount = nextExpansionCount;
      expansionStack.add(name);
      return true;
    }

    private ParsedArguments parseArguments(List<IdlToken> tokens, int index, SourceSpan span) {
      List<List<IdlToken>> arguments = new ArrayList<>();
      List<IdlToken> currentArgument = new ArrayList<>();
      int depth = 0;
      int current = index;
      if (current < tokens.size() && tokens.get(current).kind() == IdlTokenKind.RIGHT_PAREN) {
        return new ParsedArguments(true, List.of(), current);
      }
      while (current < tokens.size()) {
        IdlToken token = tokens.get(current);
        if (token.kind() == IdlTokenKind.LEFT_PAREN) {
          depth++;
          currentArgument.add(token);
        } else if (token.kind() == IdlTokenKind.RIGHT_PAREN) {
          if (depth == 0) {
            arguments.add(List.copyOf(currentArgument));
            return new ParsedArguments(true, List.copyOf(arguments), current);
          }
          depth--;
          currentArgument.add(token);
        } else if (token.kind() == IdlTokenKind.COMMA && depth == 0) {
          arguments.add(List.copyOf(currentArgument));
          currentArgument.clear();
        } else {
          currentArgument.add(token);
        }
        current++;
      }
      emitDiagnostic(
          IdlPreprocessorDiagnosticCodes.MALFORMED_MACRO,
          "Unterminated function-like IDL macro invocation",
          span);
      return ParsedArguments.invalid();
    }

    private void addDiagnostic(Diagnostic diagnostic) {
      if (diagnosticLimitReached) {
        return;
      }
      long maximum = options.diagnosticCountLimit().maximum();
      if (maximum == 0 || diagnostics.size() >= maximum) {
        diagnosticLimitReached = true;
        return;
      }
      if (!IdlPreprocessorDiagnosticCodes.DIAGNOSTIC_LIMIT_EXCEEDED.equals(diagnostic.code())
          && diagnostics.size() == maximum - 1) {
        SourceSpan span = diagnostic.span().orElse(null);
        Diagnostic limitDiagnostic =
            span == null
                ? Diagnostic.withoutSpan(
                    IdlPreprocessorDiagnosticCodes.DIAGNOSTIC_LIMIT_EXCEEDED,
                    DiagnosticSeverity.ERROR,
                    new LimitViolation(options.diagnosticCountLimit(), diagnostics.size() + 1L)
                        .message())
                : Diagnostic.withSpan(
                    IdlPreprocessorDiagnosticCodes.DIAGNOSTIC_LIMIT_EXCEEDED,
                    DiagnosticSeverity.ERROR,
                    new LimitViolation(options.diagnosticCountLimit(), diagnostics.size() + 1L)
                        .message(),
                    span);
        diagnostics.add(limitDiagnostic);
        diagnosticLimitReached = true;
        return;
      }
      diagnostics.add(diagnostic);
    }

    private void emitDiagnostic(DiagnosticCode code, String message, SourceSpan span) {
      addDiagnostic(Diagnostic.withSpan(code, DiagnosticSeverity.ERROR, message, span));
    }
  }

  private record SourceView(List<LineView> lines) {
    private SourceView {
      lines = List.copyOf(lines);
    }
  }

  private record LineView(
      int lineNumber, String lineText, List<TokenEntry> tokens, List<Diagnostic> lexerDiagnostics) {
    private LineView {
      Objects.requireNonNull(lineText, "lineText");
      tokens = List.copyOf(tokens);
      lexerDiagnostics = List.copyOf(lexerDiagnostics);
    }

    private boolean isDirective() {
      return !tokens.isEmpty() && tokens.getFirst().emitted().kind() == IdlTokenKind.HASH;
    }

    private Optional<String> directiveName() {
      if (tokens.size() < 2) {
        return Optional.empty();
      }
      IdlToken directive = tokens.get(1).emitted();
      return directive.kind() == IdlTokenKind.IDENTIFIER
          ? Optional.of(directive.lexeme())
          : Optional.empty();
    }

    private SourceSpan directiveSpan() {
      return tokens.isEmpty() ? syntheticSpan("unknown") : tokens.getFirst().emitted().span();
    }

    private String requestingSourceName() {
      return directiveSpan().start().sourceName();
    }

    private List<IdlToken> emittedTokens() {
      return tokens.stream().map(TokenEntry::emitted).toList();
    }

    private List<IdlToken> afterDirectiveTokens() {
      if (tokens.size() <= 2) {
        return List.of();
      }
      return tokens.subList(2, tokens.size()).stream().map(TokenEntry::emitted).toList();
    }

    private static SourceSpan syntheticSpan(String sourceName) {
      SourcePosition position = new SourcePosition(sourceName, 1, 1, 0);
      return new SourceSpan(position, position);
    }

    private LineView remap(ActiveLineMarkers lineMarkers) {
      List<TokenEntry> remappedTokens =
          tokens.stream()
              .map(
                  entry ->
                      new TokenEntry(
                          entry.lexed(),
                          lineMarkers.remap(entry.emitted(), lineNumber),
                          entry.line()))
              .toList();
      List<Diagnostic> remappedDiagnostics =
          lexerDiagnostics.stream()
              .map(diagnostic -> lineMarkers.remap(diagnostic, lineNumber))
              .toList();
      return new LineView(lineNumber, lineText, remappedTokens, remappedDiagnostics);
    }
  }

  private record TokenEntry(IdlToken lexed, IdlToken emitted, int line) {}

  private record IncludeDirective(String includeName, IdlIncludeKind kind) {
    private static Optional<IncludeDirective> parse(LineView line) {
      String text = line.lineText();
      int index = skipWhitespace(text, 0);
      if (index >= text.length() || text.charAt(index) != '#') {
        return Optional.empty();
      }
      index = skipWhitespace(text, index + 1);
      int nameStart = index;
      while (index < text.length() && isDirectiveNamePart(text.charAt(index))) {
        index++;
      }
      if (!"include".equals(text.substring(nameStart, index))) {
        return Optional.empty();
      }
      index = skipWhitespace(text, index);
      if (index >= text.length()) {
        return Optional.empty();
      }
      char opener = text.charAt(index);
      if (opener == '"') {
        int close = text.indexOf('"', index + 1);
        if (close <= index + 1) {
          return Optional.empty();
        }
        return Optional.of(
            new IncludeDirective(text.substring(index + 1, close), IdlIncludeKind.QUOTED));
      }
      if (opener == '<') {
        int close = text.indexOf('>', index + 1);
        if (close <= index + 1) {
          return Optional.empty();
        }
        return Optional.of(
            new IncludeDirective(text.substring(index + 1, close), IdlIncludeKind.SYSTEM));
      }
      return Optional.empty();
    }
  }

  private record LineMarkerDirective(int lineNumber, String sourceName) {
    private static boolean looksLikeLineMarker(LineView line) {
      String text = line.lineText();
      int index = skipWhitespace(text, 0);
      if (index >= text.length() || text.charAt(index) != '#') {
        return false;
      }
      index = skipWhitespace(text, index + 1);
      if (text.startsWith("line", index)) {
        int afterName = index + "line".length();
        return afterName == text.length() || Character.isWhitespace(text.charAt(afterName));
      }
      return index < text.length() && Character.isDigit(text.charAt(index));
    }

    private static Optional<LineMarkerDirective> parse(String text) {
      int index = skipWhitespace(text, 0);
      if (index >= text.length() || text.charAt(index) != '#') {
        return Optional.empty();
      }
      index = skipWhitespace(text, index + 1);
      if (text.startsWith("line", index)) {
        int afterName = index + "line".length();
        if (afterName < text.length() && !Character.isWhitespace(text.charAt(afterName))) {
          return Optional.empty();
        }
        index = skipWhitespace(text, afterName);
      }
      int lineStart = index;
      while (index < text.length() && Character.isDigit(text.charAt(index))) {
        index++;
      }
      if (lineStart == index) {
        return Optional.empty();
      }
      int lineNumber;
      try {
        lineNumber = Integer.parseInt(text.substring(lineStart, index));
      } catch (NumberFormatException exception) {
        return Optional.empty();
      }
      if (lineNumber < 1) {
        return Optional.empty();
      }

      index = skipWhitespace(text, index);
      if (index == text.length()) {
        return Optional.of(new LineMarkerDirective(lineNumber, ""));
      }
      if (text.charAt(index) != '"') {
        return Optional.empty();
      }
      int close = text.indexOf('"', index + 1);
      if (close <= index + 1) {
        return Optional.empty();
      }
      if (!text.substring(close + 1).isBlank()) {
        return Optional.empty();
      }
      return Optional.of(new LineMarkerDirective(lineNumber, text.substring(index + 1, close)));
    }
  }

  private static final class ActiveLineMarkers {
    private String sourceName;
    private int lineNumber = 1;
    private boolean identity = true;
    private boolean appliedOnCurrentLine;

    private ActiveLineMarkers(String defaultSourceName) {
      this.sourceName = requireNonBlank(defaultSourceName, "defaultSourceName");
    }

    private void apply(LineMarkerDirective directive) {
      identity = false;
      appliedOnCurrentLine = true;
      if (!directive.sourceName().isBlank()) {
        sourceName = directive.sourceName();
      }
      lineNumber = directive.lineNumber();
    }

    private void finishLine() {
      if (appliedOnCurrentLine) {
        appliedOnCurrentLine = false;
        return;
      }
      if (!identity) {
        lineNumber++;
      }
    }

    private IdlToken remap(IdlToken token, int physicalLine) {
      if (identity) {
        return token;
      }
      return new IdlToken(token.kind(), token.lexeme(), remap(token.span(), physicalLine));
    }

    private Diagnostic remap(Diagnostic diagnostic, int physicalLine) {
      if (identity || diagnostic.span().isEmpty()) {
        return diagnostic;
      }
      SourceSpan span = diagnostic.span().orElseThrow();
      return Diagnostic.withSpan(
          diagnostic.code(),
          diagnostic.severity(),
          diagnostic.message(),
          remap(span, physicalLine));
    }

    private SourceSpan remap(SourceSpan span, int physicalLine) {
      return new SourceSpan(remap(span.start(), physicalLine), remap(span.end(), physicalLine));
    }

    private SourcePosition remap(SourcePosition position, int physicalLine) {
      int logicalLine = lineNumber + Math.max(0, position.line() - physicalLine);
      return new SourcePosition(sourceName, logicalLine, position.column(), position.offset());
    }
  }

  private static final class ConditionalState {
    private final ArrayDeque<ConditionalFrame> frames = new ArrayDeque<>();

    private boolean isActive() {
      return frames.isEmpty() || frames.getLast().active();
    }

    private boolean hasOpenFrame() {
      return !frames.isEmpty();
    }

    private boolean shouldEvaluateElif() {
      if (frames.isEmpty()) {
        return false;
      }
      ConditionalFrame frame = frames.getLast();
      return frame.parentActive() && !frame.branchTaken() && !frame.sawElse();
    }

    private void push(boolean selected, SourceSpan startSpan) {
      boolean parentActive = isActive();
      frames.addLast(
          new ConditionalFrame(parentActive, parentActive && selected, selected, false, startSpan));
    }

    private Optional<String> elif(boolean selected) {
      ConditionalFrame frame = frames.removeLast();
      if (frame.sawElse()) {
        frames.addLast(frame);
        return Optional.of("IDL #elif directive cannot follow #else");
      }
      boolean active = frame.parentActive() && !frame.branchTaken() && selected;
      frames.addLast(
          new ConditionalFrame(
              frame.parentActive(),
              active,
              frame.branchTaken() || selected,
              frame.sawElse(),
              frame.startSpan()));
      return Optional.empty();
    }

    private Optional<String> elseBranch() {
      ConditionalFrame frame = frames.removeLast();
      if (frame.sawElse()) {
        frames.addLast(frame);
        return Optional.of("Duplicate IDL #else directive");
      }
      boolean active = frame.parentActive() && !frame.branchTaken();
      frames.addLast(
          new ConditionalFrame(frame.parentActive(), active, true, true, frame.startSpan()));
      return Optional.empty();
    }

    private void pop() {
      frames.removeLast();
    }

    private List<ConditionalFrame> openFrames() {
      return List.copyOf(frames);
    }
  }

  private record ConditionalFrame(
      boolean parentActive,
      boolean active,
      boolean branchTaken,
      boolean sawElse,
      SourceSpan startSpan) {}

  private record ConditionalExpressionResult(boolean value, Optional<String> diagnostic) {
    private static ConditionalExpressionResult valid(boolean value) {
      return new ConditionalExpressionResult(value, Optional.empty());
    }

    private static ConditionalExpressionResult unsupported(String diagnostic) {
      return new ConditionalExpressionResult(false, Optional.of(diagnostic));
    }
  }

  private static final class ConditionalExpression {
    private final List<IdlToken> tokens;
    private final Map<String, Macro> macros;
    private int index;

    private ConditionalExpression(List<IdlToken> tokens, Map<String, Macro> macros) {
      this.tokens = List.copyOf(tokens);
      this.macros = macros;
    }

    private ConditionalExpressionResult evaluate() {
      try {
        boolean value = parseOr();
        if (index != tokens.size()) {
          return ConditionalExpressionResult.unsupported(
              "Unsupported trailing #if expression tokens");
        }
        return ConditionalExpressionResult.valid(value);
      } catch (UnsupportedExpressionException exception) {
        return ConditionalExpressionResult.unsupported(exception.getMessage());
      }
    }

    private boolean parseOr() {
      boolean value = parseAnd();
      while (match(IdlTokenKind.LOGICAL_OR)) {
        value = parseAnd() || value;
      }
      return value;
    }

    private boolean parseAnd() {
      boolean value = parseUnary();
      while (match(IdlTokenKind.LOGICAL_AND)) {
        value = parseUnary() && value;
      }
      return value;
    }

    private boolean parseUnary() {
      if (match(IdlTokenKind.EXCLAMATION)) {
        return !parseUnary();
      }
      return parsePrimary();
    }

    private boolean parsePrimary() {
      if (match(IdlTokenKind.LEFT_PAREN)) {
        boolean value = parseOr();
        if (!match(IdlTokenKind.RIGHT_PAREN)) {
          throw new UnsupportedExpressionException("Unmatched parenthesis in #if expression");
        }
        return value;
      }
      if (peekLexeme("defined")) {
        advance();
        if (match(IdlTokenKind.LEFT_PAREN)) {
          String name = readMacroName();
          if (!match(IdlTokenKind.RIGHT_PAREN)) {
            throw new UnsupportedExpressionException("Malformed defined(NAME) expression");
          }
          return macros.containsKey(name);
        }
        return macros.containsKey(readMacroName());
      }
      if (peekKind(IdlTokenKind.INTEGER_LITERAL)) {
        String lexeme = advance().lexeme();
        return switch (lexeme) {
          case "0" -> false;
          case "1" -> true;
          default ->
              throw new UnsupportedExpressionException("Only integer 0 and 1 are supported in #if");
        };
      }
      Optional<String> macroName = current().flatMap(IdlPreprocessor::macroName);
      if (macroName.isPresent() && macros.containsKey(macroName.orElseThrow())) {
        Macro macro = macros.get(macroName.orElseThrow());
        if (!macro.functionLike() && macro.replacement().size() == 1) {
          IdlToken replacement = macro.replacement().getFirst();
          if (replacement.kind() == IdlTokenKind.INTEGER_LITERAL) {
            advance();
            return switch (replacement.lexeme()) {
              case "0" -> false;
              case "1" -> true;
              default ->
                  throw new UnsupportedExpressionException(
                      "Only object-like integer 0 and 1 macros are supported in #if");
            };
          }
        }
      }
      throw new UnsupportedExpressionException("Unsupported #if expression");
    }

    private String readMacroName() {
      Optional<String> name = current().flatMap(IdlPreprocessor::macroName);
      if (name.isEmpty()) {
        throw new UnsupportedExpressionException("Expected macro name in #if expression");
      }
      advance();
      return name.orElseThrow();
    }

    private boolean match(IdlTokenKind kind) {
      if (!peekKind(kind)) {
        return false;
      }
      index++;
      return true;
    }

    private boolean peekKind(IdlTokenKind kind) {
      return current().map(token -> token.kind() == kind).orElse(false);
    }

    private boolean peekLexeme(String lexeme) {
      return current().map(token -> token.lexeme().equals(lexeme)).orElse(false);
    }

    private IdlToken advance() {
      return tokens.get(index++);
    }

    private Optional<IdlToken> current() {
      return index < tokens.size() ? Optional.of(tokens.get(index)) : Optional.empty();
    }
  }

  private static final class UnsupportedExpressionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private UnsupportedExpressionException(String message) {
      super(message);
    }
  }

  private record Macro(
      String name, List<String> parameters, List<IdlToken> replacement, boolean functionLike) {
    private Macro {
      name = requireNonBlank(name, "name");
      parameters = List.copyOf(parameters);
      replacement = List.copyOf(replacement);
    }

    private List<IdlToken> expand(List<List<IdlToken>> arguments, SourceSpan invocationSpan) {
      Map<String, List<IdlToken>> argumentByName = new HashMap<>();
      for (int index = 0; index < parameters.size(); index++) {
        argumentByName.put(parameters.get(index), arguments.get(index));
      }
      List<IdlToken> expanded = new ArrayList<>();
      for (IdlToken token : replacement) {
        Optional<String> tokenName = macroName(token);
        if (tokenName.isPresent() && argumentByName.containsKey(tokenName.orElseThrow())) {
          expanded.addAll(argumentByName.get(tokenName.orElseThrow()));
        } else {
          expanded.add(withSpan(token, invocationSpan));
        }
      }
      return expanded;
    }
  }

  private record ParsedParameters(boolean valid, List<String> parameters, int bodyStart) {
    private ParsedParameters {
      parameters = List.copyOf(parameters);
    }

    private static ParsedParameters invalid() {
      return new ParsedParameters(false, List.of(), -1);
    }
  }

  private record ParsedArguments(boolean valid, List<List<IdlToken>> arguments, int endIndex) {
    private ParsedArguments {
      arguments = arguments.stream().map(List::copyOf).toList();
    }

    private static ParsedArguments invalid() {
      return new ParsedArguments(false, List.of(), -1);
    }
  }

  private record NormalizedSource(
      String sourceName,
      String text,
      List<SourcePosition> positions,
      SourcePosition eofPosition,
      List<String> lineTexts) {
    private NormalizedSource {
      sourceName = requireNonBlank(sourceName, "sourceName");
      Objects.requireNonNull(text, "text");
      positions = List.copyOf(positions);
      Objects.requireNonNull(eofPosition, "eofPosition");
      lineTexts = List.copyOf(lineTexts);
    }

    private static NormalizedSource from(IdlSource source) {
      StringBuilder text = new StringBuilder();
      List<SourcePosition> positions = new ArrayList<>();
      int line = 1;
      int column = 1;
      int offset = 0;
      String input = source.sourceText();
      while (offset < input.length()) {
        char current = input.charAt(offset);
        if (current == '\\' && offset + 1 < input.length() && isNewlineStart(input, offset + 1)) {
          offset++;
          if (input.charAt(offset) == '\r'
              && offset + 1 < input.length()
              && input.charAt(offset + 1) == '\n') {
            offset += 2;
          } else {
            offset++;
          }
          line++;
          column = 1;
          continue;
        }
        if (current == '\r' && offset + 1 < input.length() && input.charAt(offset + 1) == '\n') {
          text.append('\r');
          positions.add(new SourcePosition(source.sourceName(), line, column, offset));
          text.append('\n');
          positions.add(new SourcePosition(source.sourceName(), line, column + 1, offset + 1));
          offset += 2;
          line++;
          column = 1;
          continue;
        }
        text.append(current);
        positions.add(new SourcePosition(source.sourceName(), line, column, offset));
        offset++;
        if (current == '\r' || current == '\n') {
          line++;
          column = 1;
        } else {
          column++;
        }
      }
      SourcePosition eofPosition = new SourcePosition(source.sourceName(), line, column, offset);
      String normalizedText = text.toString();
      List<String> lineTexts = splitLineTexts(normalizedText);
      return new NormalizedSource(
          source.sourceName(), normalizedText, positions, eofPosition, lineTexts);
    }

    private SourcePosition map(long offset) {
      if (offset >= positions.size()) {
        return eofPosition;
      }
      return positions.get(Math.toIntExact(offset));
    }

    private IdlToken remap(IdlToken token) {
      return new IdlToken(
          token.kind(),
          token.lexeme(),
          new SourceSpan(map(token.span().start().offset()), map(token.span().end().offset())));
    }

    private Diagnostic remap(Diagnostic diagnostic) {
      if (diagnostic.span().isEmpty()) {
        return diagnostic;
      }
      SourceSpan span = diagnostic.span().orElseThrow();
      SourceSpan remapped = new SourceSpan(map(span.start().offset()), map(span.end().offset()));
      return Diagnostic.withSpan(
          diagnostic.code(), diagnostic.severity(), diagnostic.message(), remapped);
    }

    private Set<Integer> directiveLines() {
      Set<Integer> lines = new HashSet<>();
      for (int index = 0; index < lineTexts.size(); index++) {
        String line = lineTexts.get(index);
        int first = skipWhitespace(line, 0);
        if (first < line.length() && line.charAt(first) == '#') {
          lines.add(index + 1);
        }
      }
      return lines;
    }

    private String lineText(int lineNumber) {
      if (lineNumber < 1 || lineNumber > lineTexts.size()) {
        return "";
      }
      return lineTexts.get(lineNumber - 1);
    }
  }

  private static List<String> splitLineTexts(String text) {
    List<String> lines = new ArrayList<>();
    StringBuilder line = new StringBuilder();
    int index = 0;
    while (index < text.length()) {
      char current = text.charAt(index);
      if (current == '\r') {
        if (index + 1 < text.length() && text.charAt(index + 1) == '\n') {
          index++;
        }
        lines.add(line.toString());
        line.setLength(0);
      } else if (current == '\n') {
        lines.add(line.toString());
        line.setLength(0);
      } else {
        line.append(current);
      }
      index++;
    }
    lines.add(line.toString());
    return lines;
  }

  private static boolean isNewlineStart(String source, int offset) {
    char current = source.charAt(offset);
    return current == '\r' || current == '\n';
  }

  private static IdlToken withSpan(IdlToken token, SourceSpan span) {
    return new IdlToken(token.kind(), token.lexeme(), span);
  }

  private static Optional<String> macroName(IdlToken token) {
    if (isMacroNameToken(token)) {
      return Optional.of(token.lexeme());
    }
    return Optional.empty();
  }

  private static boolean isMacroNameToken(IdlToken token) {
    return token.kind() == IdlTokenKind.IDENTIFIER
        || token.kind() == IdlTokenKind.ESCAPED_IDENTIFIER;
  }

  private static int skipWhitespace(String text, int index) {
    int current = index;
    while (current < text.length() && Character.isWhitespace(text.charAt(current))) {
      current++;
    }
    return current;
  }

  private static boolean isDirectiveNamePart(char value) {
    return (value >= 'A' && value <= 'Z')
        || (value >= 'a' && value <= 'z')
        || (value >= '0' && value <= '9')
        || value == '_';
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
