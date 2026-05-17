package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for an IDL exception declaration.
 *
 * @param name exception identifier
 * @param fields fields in encounter order
 * @param span source span covered by the exception declaration
 */
public record IdlExceptionDeclaration(String name, List<IdlField> fields, SourceSpan span)
    implements IdlDeclaration {

  /** Creates a validated exception-declaration node. */
  public IdlExceptionDeclaration {
    name = IdlAstValidation.requireNonBlank(name, "name");
    fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    Objects.requireNonNull(span, "span");
  }
}
