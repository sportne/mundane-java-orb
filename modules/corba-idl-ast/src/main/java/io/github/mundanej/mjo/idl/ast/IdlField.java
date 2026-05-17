package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;

/**
 * Immutable AST node for a field inside a struct or exception body.
 *
 * @param type field type reference
 * @param name field identifier
 * @param span source span covered by the field declarator
 */
public record IdlField(IdlTypeReference type, String name, SourceSpan span) implements IdlAstNode {

  /** Creates a validated field node. */
  public IdlField {
    Objects.requireNonNull(type, "type");
    name = IdlAstValidation.requireNonBlank(name, "name");
    Objects.requireNonNull(span, "span");
  }
}
