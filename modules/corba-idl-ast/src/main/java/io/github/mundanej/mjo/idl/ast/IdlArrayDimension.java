package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;

/**
 * Immutable AST node for one fixed-array declarator dimension.
 *
 * @param size constant expression that defines the dimension size
 * @param span source span covered by the dimension
 */
public record IdlArrayDimension(IdlConstantExpression size, SourceSpan span) implements IdlAstNode {

  /** Creates a validated array-dimension node. */
  public IdlArrayDimension {
    Objects.requireNonNull(size, "size");
    Objects.requireNonNull(span, "span");
  }
}
