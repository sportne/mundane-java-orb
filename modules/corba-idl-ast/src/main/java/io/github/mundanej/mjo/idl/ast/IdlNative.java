package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;

/**
 * Immutable AST node for an IDL native declaration.
 *
 * @param name native type identifier
 * @param span source span covered by the declaration
 */
public record IdlNative(String name, SourceSpan span) implements IdlDeclaration {

  /** Creates a validated native declaration node. */
  public IdlNative {
    name = IdlAstValidation.requireNonBlank(name, "name");
    Objects.requireNonNull(span, "span");
  }
}
