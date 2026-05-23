package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable AST node for an IDL union case label.
 *
 * @param defaultLabel whether this is the default label
 * @param expression constant expression for non-default labels
 * @param span source span covered by the label
 */
public record IdlUnionLabel(
    boolean defaultLabel, Optional<IdlConstantExpression> expression, SourceSpan span)
    implements IdlAstNode {

  /** Creates a validated union-label node. */
  public IdlUnionLabel {
    Objects.requireNonNull(expression, "expression");
    Objects.requireNonNull(span, "span");
    if (defaultLabel && expression.isPresent()) {
      throw new IllegalArgumentException("default labels must not have expressions");
    }
    if (!defaultLabel && expression.isEmpty()) {
      throw new IllegalArgumentException("case labels must have expressions");
    }
  }

  /** Creates a non-default case label. */
  public static IdlUnionLabel caseLabel(IdlConstantExpression expression, SourceSpan span) {
    return new IdlUnionLabel(false, Optional.of(expression), span);
  }

  /** Creates a default case label. */
  public static IdlUnionLabel defaultLabel(SourceSpan span) {
    return new IdlUnionLabel(true, Optional.empty(), span);
  }
}
