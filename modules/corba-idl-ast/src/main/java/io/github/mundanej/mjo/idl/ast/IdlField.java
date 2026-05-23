package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for a field inside a struct, exception, or union body.
 *
 * @param type field type reference
 * @param declarator field declarator
 * @param span source span covered by the field declarator
 */
public record IdlField(IdlTypeReference type, IdlDeclarator declarator, SourceSpan span)
    implements IdlAstNode {

  /** Creates a validated field node. */
  public IdlField {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(declarator, "declarator");
    Objects.requireNonNull(span, "span");
  }

  /** Creates a simple non-array field. */
  public IdlField(IdlTypeReference type, String name, SourceSpan span) {
    this(type, new IdlDeclarator(name, span), span);
  }

  /** Returns the field identifier. */
  public String name() {
    return declarator.name();
  }

  /** Returns fixed-array dimensions for this field. */
  public List<IdlArrayDimension> dimensions() {
    return declarator.dimensions();
  }
}
