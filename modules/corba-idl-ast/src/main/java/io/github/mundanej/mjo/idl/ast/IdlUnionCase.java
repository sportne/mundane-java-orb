package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for one IDL union case declaration.
 *
 * @param labels case labels that select this member
 * @param type member type
 * @param declarator member declarator
 * @param span source span covered by the case declaration
 */
public record IdlUnionCase(
    List<IdlUnionLabel> labels, IdlTypeReference type, IdlDeclarator declarator, SourceSpan span)
    implements IdlAstNode {

  /** Creates a validated union-case node. */
  public IdlUnionCase {
    labels = List.copyOf(Objects.requireNonNull(labels, "labels"));
    if (labels.isEmpty()) {
      throw new IllegalArgumentException("labels must not be empty");
    }
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(declarator, "declarator");
    Objects.requireNonNull(span, "span");
  }
}
