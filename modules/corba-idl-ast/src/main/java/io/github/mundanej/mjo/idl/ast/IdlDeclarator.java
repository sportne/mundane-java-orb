package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for simple or fixed-array IDL declarators.
 *
 * @param name declarator identifier
 * @param dimensions fixed-array dimensions in encounter order
 * @param span source span covered by the declarator
 */
public record IdlDeclarator(String name, List<IdlArrayDimension> dimensions, SourceSpan span)
    implements IdlAstNode {

  /** Creates a validated declarator node. */
  public IdlDeclarator {
    name = IdlAstValidation.requireNonBlank(name, "name");
    dimensions = List.copyOf(Objects.requireNonNull(dimensions, "dimensions"));
    Objects.requireNonNull(span, "span");
  }

  /** Creates a simple declarator with no array dimensions. */
  public IdlDeclarator(String name, SourceSpan span) {
    this(name, List.of(), span);
  }

  /** Returns whether this declarator has fixed-array dimensions. */
  public boolean array() {
    return !dimensions.isEmpty();
  }
}
