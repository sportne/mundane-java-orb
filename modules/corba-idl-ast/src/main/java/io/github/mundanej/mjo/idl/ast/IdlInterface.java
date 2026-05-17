package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for a full IDL interface declaration.
 *
 * @param name interface identifier
 * @param members operations and attributes in encounter order
 * @param span source span covered by the interface declaration
 */
public record IdlInterface(String name, List<IdlInterfaceMember> members, SourceSpan span)
    implements IdlDeclaration {

  /** Creates a validated interface node. */
  public IdlInterface {
    name = IdlAstValidation.requireNonBlank(name, "name");
    members = List.copyOf(Objects.requireNonNull(members, "members"));
    Objects.requireNonNull(span, "span");
  }
}
