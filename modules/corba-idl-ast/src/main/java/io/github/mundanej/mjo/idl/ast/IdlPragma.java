package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for repository-affecting IDL pragma directives.
 *
 * @param name pragma name such as {@code prefix}, {@code ID}, {@code version}, {@code typeid}, or
 *     {@code typeprefix}
 * @param arguments normalized pragma arguments in encounter order
 * @param span source span covered by the pragma
 */
public record IdlPragma(String name, List<String> arguments, SourceSpan span)
    implements IdlDeclaration {

  /** Creates a validated pragma node. */
  public IdlPragma {
    name = IdlAstValidation.requireNonBlank(name, "name");
    arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
    arguments.forEach(argument -> IdlAstValidation.requireNonBlank(argument, "argument"));
    Objects.requireNonNull(span, "span");
  }
}
