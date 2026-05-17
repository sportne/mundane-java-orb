package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST root for one preprocessed IDL translation unit.
 *
 * @param declarations declarations in encounter order
 * @param span source span covered by the translation unit
 */
public record IdlTranslationUnit(List<IdlDeclaration> declarations, SourceSpan span)
    implements IdlAstNode {

  /** Creates a validated translation-unit node. */
  public IdlTranslationUnit {
    declarations = List.copyOf(Objects.requireNonNull(declarations, "declarations"));
    Objects.requireNonNull(span, "span");
  }
}
