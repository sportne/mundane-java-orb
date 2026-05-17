package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for an IDL attribute declaration.
 *
 * @param readonly whether the attribute uses the `readonly` modifier
 * @param type attribute type reference
 * @param names declared attribute identifiers
 * @param span source span covered by the attribute declaration
 */
public record IdlAttribute(
    boolean readonly, IdlTypeReference type, List<String> names, SourceSpan span)
    implements IdlInterfaceMember {

  /** Creates a validated attribute node. */
  public IdlAttribute {
    Objects.requireNonNull(type, "type");
    names = List.copyOf(Objects.requireNonNull(names, "names"));
    if (names.isEmpty()) {
      throw new IllegalArgumentException("names must not be empty");
    }
    Objects.requireNonNull(span, "span");
  }
}
