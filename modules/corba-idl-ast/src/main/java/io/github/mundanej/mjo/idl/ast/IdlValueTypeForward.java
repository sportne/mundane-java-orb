package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;

/**
 * Immutable AST node for an IDL valuetype forward declaration.
 *
 * @param abstractValue whether the forward declaration is abstract
 * @param name valuetype identifier
 * @param span source span covered by the declaration
 */
public record IdlValueTypeForward(boolean abstractValue, String name, SourceSpan span)
    implements IdlDeclaration {

  /** Creates a validated valuetype forward node. */
  public IdlValueTypeForward {
    name = IdlAstValidation.requireNonBlank(name, "name");
    Objects.requireNonNull(span, "span");
  }
}
