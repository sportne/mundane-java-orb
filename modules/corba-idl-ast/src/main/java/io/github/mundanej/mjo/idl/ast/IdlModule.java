package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for an IDL module declaration.
 *
 * @param name module identifier
 * @param declarations declarations inside the module body
 * @param span source span covered by the module declaration
 */
public record IdlModule(String name, List<IdlDeclaration> declarations, SourceSpan span)
    implements IdlDeclaration {

  /** Creates a validated module node. */
  public IdlModule {
    name = IdlAstValidation.requireNonBlank(name, "name");
    declarations = List.copyOf(Objects.requireNonNull(declarations, "declarations"));
    Objects.requireNonNull(span, "span");
  }
}
