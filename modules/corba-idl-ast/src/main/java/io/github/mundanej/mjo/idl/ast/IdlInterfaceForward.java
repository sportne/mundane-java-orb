package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;

/**
 * Immutable AST node for an IDL interface forward declaration.
 *
 * @param name interface identifier
 * @param span source span covered by the forward declaration
 */
public record IdlInterfaceForward(String name, SourceSpan span) implements IdlDeclaration {

  /** Creates a validated interface-forward node. */
  public IdlInterfaceForward {
    name = IdlAstValidation.requireNonBlank(name, "name");
    Objects.requireNonNull(span, "span");
  }
}
