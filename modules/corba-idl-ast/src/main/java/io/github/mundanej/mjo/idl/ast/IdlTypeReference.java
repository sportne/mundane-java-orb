package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable AST node for a syntactic IDL type reference.
 *
 * <p>The {@link #name()} accessor remains the normalized spelling used by the earlier parser slice.
 * Constructed types expose their structure through {@link #kind()}, {@link #elementType()}, and
 * {@link #bound()} while still returning deterministic normalized names such as {@code
 * sequence<long, 4>}.
 */
public final class IdlTypeReference implements IdlAstNode {

  private final IdlTypeReferenceKind kind;
  private final String name;
  private final Optional<IdlTypeReference> elementType;
  private final Optional<IdlConstantExpression> bound;
  private final SourceSpan span;

  /** Creates a simple named type reference. */
  public IdlTypeReference(String name, SourceSpan span) {
    this(IdlTypeReferenceKind.NAMED, name, Optional.empty(), Optional.empty(), span);
  }

  private IdlTypeReference(
      IdlTypeReferenceKind kind,
      String name,
      Optional<IdlTypeReference> elementType,
      Optional<IdlConstantExpression> bound,
      SourceSpan span) {
    this.kind = Objects.requireNonNull(kind, "kind");
    this.name = IdlAstValidation.requireNonBlank(name, "name");
    this.elementType = Objects.requireNonNull(elementType, "elementType");
    this.bound = Objects.requireNonNull(bound, "bound");
    this.span = Objects.requireNonNull(span, "span");
  }

  /** Creates an unbounded sequence type reference. */
  public static IdlTypeReference sequence(IdlTypeReference elementType, SourceSpan span) {
    Objects.requireNonNull(elementType, "elementType");
    return new IdlTypeReference(
        IdlTypeReferenceKind.SEQUENCE,
        "sequence<" + elementType.name() + ">",
        Optional.of(elementType),
        Optional.empty(),
        span);
  }

  /** Creates a bounded sequence type reference. */
  public static IdlTypeReference sequence(
      IdlTypeReference elementType, IdlConstantExpression bound, SourceSpan span) {
    Objects.requireNonNull(elementType, "elementType");
    Objects.requireNonNull(bound, "bound");
    return new IdlTypeReference(
        IdlTypeReferenceKind.SEQUENCE,
        "sequence<" + elementType.name() + ", " + String.join(" ", bound.lexemes()) + ">",
        Optional.of(elementType),
        Optional.of(bound),
        span);
  }

  /** Creates a bounded string or wstring type reference. */
  public static IdlTypeReference boundedString(
      String keyword, IdlConstantExpression bound, SourceSpan span) {
    if (!keyword.equals("string") && !keyword.equals("wstring")) {
      throw new IllegalArgumentException("keyword must be string or wstring");
    }
    Objects.requireNonNull(bound, "bound");
    return new IdlTypeReference(
        IdlTypeReferenceKind.BOUNDED_STRING,
        keyword + "<" + String.join(" ", bound.lexemes()) + ">",
        Optional.empty(),
        Optional.of(bound),
        span);
  }

  /** Returns the structural type-reference kind. */
  public IdlTypeReferenceKind kind() {
    return kind;
  }

  /** Returns the normalized type spelling. */
  public String name() {
    return name;
  }

  /** Returns the element type for sequence references. */
  public Optional<IdlTypeReference> elementType() {
    return elementType;
  }

  /** Returns the bound expression for bounded sequence/string references. */
  public Optional<IdlConstantExpression> bound() {
    return bound;
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
    if (!(other instanceof IdlTypeReference that)) {
      return false;
    }
    return kind == that.kind
        && name.equals(that.name)
        && elementType.equals(that.elementType)
        && bound.equals(that.bound)
        && span.equals(that.span);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, name, elementType, bound, span);
  }

  @Override
  public String toString() {
    return "IdlTypeReference[kind=%s, name=%s, elementType=%s, bound=%s, span=%s]"
        .formatted(kind, name, elementType, bound, span);
  }
}
