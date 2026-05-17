package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for an IDL enum declaration.
 *
 * @param name enum identifier
 * @param enumerators enumerator identifiers in encounter order
 * @param span source span covered by the enum declaration
 */
public record IdlEnum(String name, List<String> enumerators, SourceSpan span)
    implements IdlDeclaration {

  /** Creates a validated enum node. */
  public IdlEnum {
    name = IdlAstValidation.requireNonBlank(name, "name");
    enumerators = List.copyOf(Objects.requireNonNull(enumerators, "enumerators"));
    if (enumerators.isEmpty()) {
      throw new IllegalArgumentException("enumerators must not be empty");
    }
    Objects.requireNonNull(span, "span");
  }
}
