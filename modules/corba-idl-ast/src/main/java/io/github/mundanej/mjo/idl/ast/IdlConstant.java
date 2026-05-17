package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;

/**
 * Immutable AST node for an IDL constant declaration.
 *
 * @param type constant type reference
 * @param name constant identifier
 * @param expression unevaluated constant expression
 * @param span source span covered by the constant declaration
 */
public record IdlConstant(
    IdlTypeReference type, String name, IdlConstantExpression expression, SourceSpan span)
    implements IdlDeclaration {

  /** Creates a validated constant node. */
  public IdlConstant {
    Objects.requireNonNull(type, "type");
    name = IdlAstValidation.requireNonBlank(name, "name");
    Objects.requireNonNull(expression, "expression");
    Objects.requireNonNull(span, "span");
  }
}
