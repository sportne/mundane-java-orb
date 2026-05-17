package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for an unevaluated IDL constant expression.
 *
 * @param lexemes expression token lexemes in encounter order
 * @param span source span covered by the expression
 */
public record IdlConstantExpression(List<String> lexemes, SourceSpan span) implements IdlAstNode {

  /** Creates a validated constant-expression node. */
  public IdlConstantExpression {
    lexemes = List.copyOf(Objects.requireNonNull(lexemes, "lexemes"));
    if (lexemes.isEmpty()) {
      throw new IllegalArgumentException("lexemes must not be empty");
    }
    Objects.requireNonNull(span, "span");
  }
}
