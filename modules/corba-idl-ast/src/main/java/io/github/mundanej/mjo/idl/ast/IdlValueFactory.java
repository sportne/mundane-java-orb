package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for an IDL valuetype factory declaration.
 *
 * @param name factory identifier
 * @param parameters factory parameters in encounter order
 * @param raises exception scoped names listed in a {@code raises} clause
 * @param span source span covered by the factory declaration
 */
public record IdlValueFactory(
    String name, List<IdlParameter> parameters, List<String> raises, SourceSpan span)
    implements IdlValueMember {

  /** Creates a validated valuetype factory node. */
  public IdlValueFactory {
    name = IdlAstValidation.requireNonBlank(name, "name");
    parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
    raises = List.copyOf(Objects.requireNonNull(raises, "raises"));
    raises.forEach(raise -> IdlAstValidation.requireNonBlank(raise, "raises"));
    Objects.requireNonNull(span, "span");
  }
}
