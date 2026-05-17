package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;

/**
 * Immutable AST node for a syntactic IDL type reference.
 *
 * @param name type keyword or scoped name exactly normalized by the parser
 * @param span source span covered by the type reference
 */
public record IdlTypeReference(String name, SourceSpan span) implements IdlAstNode {

  /** Creates a validated type-reference node. */
  public IdlTypeReference {
    name = IdlAstValidation.requireNonBlank(name, "name");
    Objects.requireNonNull(span, "span");
  }
}
