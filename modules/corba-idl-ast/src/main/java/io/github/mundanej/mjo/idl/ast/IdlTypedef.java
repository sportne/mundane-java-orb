package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for an IDL typedef declaration.
 *
 * @param type aliased type reference
 * @param declarators alias declarators in encounter order
 * @param span source span covered by the typedef declaration
 */
public record IdlTypedef(IdlTypeReference type, List<IdlDeclarator> declarators, SourceSpan span)
    implements IdlDeclaration {

  /** Creates a validated typedef node. */
  public IdlTypedef {
    Objects.requireNonNull(type, "type");
    declarators = List.copyOf(Objects.requireNonNull(declarators, "declarators"));
    if (declarators.isEmpty()) {
      throw new IllegalArgumentException("declarators must not be empty");
    }
    Objects.requireNonNull(span, "span");
  }
}
