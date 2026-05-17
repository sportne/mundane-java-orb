package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for an IDL struct declaration.
 *
 * @param name struct identifier
 * @param fields fields in encounter order
 * @param span source span covered by the struct declaration
 */
public record IdlStruct(String name, List<IdlField> fields, SourceSpan span)
    implements IdlDeclaration {

  /** Creates a validated struct node. */
  public IdlStruct {
    name = IdlAstValidation.requireNonBlank(name, "name");
    fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    Objects.requireNonNull(span, "span");
  }
}
