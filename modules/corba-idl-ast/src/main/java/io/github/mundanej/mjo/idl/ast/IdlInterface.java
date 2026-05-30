package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for a full IDL interface declaration.
 *
 * @param kind interface modifier category
 * @param name interface identifier
 * @param baseInterfaces inherited interface scoped names in encounter order
 * @param members operations and attributes in encounter order
 * @param span source span covered by the interface declaration
 */
public record IdlInterface(
    IdlInterfaceKind kind,
    String name,
    List<String> baseInterfaces,
    List<IdlInterfaceMember> members,
    SourceSpan span)
    implements IdlDeclaration {

  /** Creates a validated interface node. */
  public IdlInterface {
    Objects.requireNonNull(kind, "kind");
    name = IdlAstValidation.requireNonBlank(name, "name");
    baseInterfaces = List.copyOf(Objects.requireNonNull(baseInterfaces, "baseInterfaces"));
    members = List.copyOf(Objects.requireNonNull(members, "members"));
    Objects.requireNonNull(span, "span");
  }

  /** Creates an ordinary interface node. */
  public IdlInterface(
      String name, List<String> baseInterfaces, List<IdlInterfaceMember> members, SourceSpan span) {
    this(IdlInterfaceKind.NORMAL, name, baseInterfaces, members, span);
  }

  /** Creates an interface with no inherited bases. */
  public IdlInterface(String name, List<IdlInterfaceMember> members, SourceSpan span) {
    this(IdlInterfaceKind.NORMAL, name, List.of(), members, span);
  }
}
