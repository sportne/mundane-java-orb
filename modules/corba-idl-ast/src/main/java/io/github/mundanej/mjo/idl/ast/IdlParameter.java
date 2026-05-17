package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;

/**
 * Immutable AST node for an IDL operation parameter.
 *
 * @param direction parameter passing direction
 * @param type parameter type reference
 * @param name parameter identifier
 * @param span source span covered by the parameter declaration
 */
public record IdlParameter(
    IdlParameterDirection direction, IdlTypeReference type, String name, SourceSpan span)
    implements IdlAstNode {

  /** Creates a validated parameter node. */
  public IdlParameter {
    Objects.requireNonNull(direction, "direction");
    Objects.requireNonNull(type, "type");
    name = IdlAstValidation.requireNonBlank(name, "name");
    Objects.requireNonNull(span, "span");
  }
}
