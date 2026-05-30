package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node for an IDL attribute declaration.
 *
 * <p>The public constructor keeps the earlier simple-name surface. Parser paths that need
 * fixed-array declarators use {@link #withDeclarators}.
 */
public final class IdlAttribute implements IdlInterfaceMember, IdlValueMember {

  private final boolean readonly;
  private final IdlTypeReference type;
  private final List<IdlDeclarator> declarators;
  private final SourceSpan span;

  /** Creates an attribute from simple non-array names. */
  public IdlAttribute(
      boolean readonly, IdlTypeReference type, List<String> names, SourceSpan span) {
    this(
        readonly,
        type,
        names.stream().map(name -> new IdlDeclarator(name, span)).toList(),
        span,
        true);
  }

  private IdlAttribute(
      boolean readonly,
      IdlTypeReference type,
      List<IdlDeclarator> declarators,
      SourceSpan span,
      boolean ignored) {
    this.readonly = readonly;
    this.type = Objects.requireNonNull(type, "type");
    this.declarators = List.copyOf(Objects.requireNonNull(declarators, "declarators"));
    if (this.declarators.isEmpty()) {
      throw new IllegalArgumentException("declarators must not be empty");
    }
    this.span = Objects.requireNonNull(span, "span");
  }

  /** Creates an attribute from parsed declarators. */
  public static IdlAttribute withDeclarators(
      boolean readonly, IdlTypeReference type, List<IdlDeclarator> declarators, SourceSpan span) {
    return new IdlAttribute(readonly, type, declarators, span, true);
  }

  /** Returns whether the attribute is readonly. */
  public boolean readonly() {
    return readonly;
  }

  /** Returns the attribute type reference. */
  public IdlTypeReference type() {
    return type;
  }

  /** Returns attribute declarators in encounter order. */
  public List<IdlDeclarator> declarators() {
    return declarators;
  }

  /** Returns attribute names in encounter order. */
  public List<String> names() {
    return declarators.stream().map(IdlDeclarator::name).toList();
  }

  @Override
  public SourceSpan span() {
    return span;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof IdlAttribute that)) {
      return false;
    }
    return readonly == that.readonly
        && type.equals(that.type)
        && declarators.equals(that.declarators)
        && span.equals(that.span);
  }

  @Override
  public int hashCode() {
    return Objects.hash(readonly, type, declarators, span);
  }

  @Override
  public String toString() {
    return "IdlAttribute[readonly=%s, type=%s, declarators=%s, span=%s]"
        .formatted(readonly, type, declarators, span);
  }
}
