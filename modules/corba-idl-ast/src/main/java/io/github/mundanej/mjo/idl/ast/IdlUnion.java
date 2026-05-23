package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for an IDL union declaration.
 *
 * @param name union identifier
 * @param discriminatorType switch discriminator type
 * @param cases union cases in encounter order
 * @param span source span covered by the union declaration
 */
public record IdlUnion(
    String name, IdlTypeReference discriminatorType, List<IdlUnionCase> cases, SourceSpan span)
    implements IdlDeclaration {

  /** Creates a validated union node. */
  public IdlUnion {
    name = IdlAstValidation.requireNonBlank(name, "name");
    Objects.requireNonNull(discriminatorType, "discriminatorType");
    cases = List.copyOf(Objects.requireNonNull(cases, "cases"));
    if (cases.isEmpty()) {
      throw new IllegalArgumentException("cases must not be empty");
    }
    Objects.requireNonNull(span, "span");
  }
}
