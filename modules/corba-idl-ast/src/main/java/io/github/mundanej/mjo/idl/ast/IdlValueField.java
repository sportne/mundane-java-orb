package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for a public or private valuetype state member declaration.
 *
 * @param visibility state member visibility
 * @param type declared member type
 * @param declarators member declarators in encounter order
 * @param span source span covered by the declaration
 */
public record IdlValueField(
    IdlValueVisibility visibility,
    IdlTypeReference type,
    List<IdlDeclarator> declarators,
    SourceSpan span)
    implements IdlValueMember {

  /** Creates a validated valuetype state member node. */
  public IdlValueField {
    Objects.requireNonNull(visibility, "visibility");
    Objects.requireNonNull(type, "type");
    declarators = List.copyOf(Objects.requireNonNull(declarators, "declarators"));
    if (declarators.isEmpty()) {
      throw new IllegalArgumentException("declarators must not be empty");
    }
    Objects.requireNonNull(span, "span");
  }
}
