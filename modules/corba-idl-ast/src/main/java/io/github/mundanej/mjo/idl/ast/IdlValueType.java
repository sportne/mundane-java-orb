package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for a full IDL valuetype declaration.
 *
 * @param custom whether the valuetype uses the {@code custom} modifier
 * @param abstractValue whether the valuetype uses the {@code abstract} modifier
 * @param name valuetype identifier
 * @param baseValueTypes inherited valuetype scoped names in encounter order
 * @param supportedInterfaces supported interface scoped names in encounter order
 * @param members valuetype body members in encounter order
 * @param span source span covered by the declaration
 */
public record IdlValueType(
    boolean custom,
    boolean abstractValue,
    String name,
    List<String> baseValueTypes,
    List<String> supportedInterfaces,
    List<IdlValueMember> members,
    SourceSpan span)
    implements IdlDeclaration {

  /** Creates a validated valuetype node. */
  public IdlValueType {
    name = IdlAstValidation.requireNonBlank(name, "name");
    baseValueTypes = List.copyOf(Objects.requireNonNull(baseValueTypes, "baseValueTypes"));
    baseValueTypes.forEach(base -> IdlAstValidation.requireNonBlank(base, "baseValueTypes"));
    supportedInterfaces =
        List.copyOf(Objects.requireNonNull(supportedInterfaces, "supportedInterfaces"));
    supportedInterfaces.forEach(
        supported -> IdlAstValidation.requireNonBlank(supported, "supportedInterfaces"));
    members = List.copyOf(Objects.requireNonNull(members, "members"));
    Objects.requireNonNull(span, "span");
  }
}
