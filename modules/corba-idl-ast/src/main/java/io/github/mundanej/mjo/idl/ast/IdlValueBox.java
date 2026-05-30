package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;

/**
 * Immutable AST node for an IDL value box declaration.
 *
 * @param name value box identifier
 * @param boxedType boxed IDL type reference
 * @param span source span covered by the declaration
 */
public record IdlValueBox(String name, IdlTypeReference boxedType, SourceSpan span)
    implements IdlDeclaration {

  /** Creates a validated value box node. */
  public IdlValueBox {
    name = IdlAstValidation.requireNonBlank(name, "name");
    Objects.requireNonNull(boxedType, "boxedType");
    Objects.requireNonNull(span, "span");
  }
}
