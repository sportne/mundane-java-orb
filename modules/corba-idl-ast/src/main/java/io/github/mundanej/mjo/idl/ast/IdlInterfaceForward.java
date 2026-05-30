package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;

/**
 * Immutable AST node for an IDL interface forward declaration.
 *
 * @param kind interface modifier category
 * @param name interface identifier
 * @param span source span covered by the forward declaration
 */
public record IdlInterfaceForward(IdlInterfaceKind kind, String name, SourceSpan span)
    implements IdlDeclaration {

  /** Creates a validated interface-forward node. */
  public IdlInterfaceForward {
    Objects.requireNonNull(kind, "kind");
    name = IdlAstValidation.requireNonBlank(name, "name");
    Objects.requireNonNull(span, "span");
  }

  /** Creates an ordinary interface-forward node. */
  public IdlInterfaceForward(String name, SourceSpan span) {
    this(IdlInterfaceKind.NORMAL, name, span);
  }
}
