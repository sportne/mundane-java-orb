package io.github.mundanej.mjo.idl.semantics;

import io.github.mundanej.mjo.common.SourceSpan;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable semantic symbol emitted by IDL semantic analysis.
 *
 * @param kind symbol category
 * @param name simple IDL name
 * @param qualifiedName absolute IDL name beginning with {@code ::}
 * @param typeName syntactic type name for typed symbols
 * @param resolvedTypeName resolved builtin type or absolute user-defined type name
 * @param constantValue evaluated value for constants and enum values
 * @param repositoryId repository ID assigned by default rules or repository pragmas
 * @param span source span for the declaration that introduced the symbol
 */
public record IdlSymbol(
    IdlSymbolKind kind,
    String name,
    String qualifiedName,
    Optional<String> typeName,
    Optional<String> resolvedTypeName,
    Optional<IdlConstantValue> constantValue,
    Optional<String> repositoryId,
    SourceSpan span) {

  /** Creates a validated semantic symbol. */
  public IdlSymbol {
    Objects.requireNonNull(kind, "kind");
    name = requireNonBlank(name, "name");
    qualifiedName = requireAbsoluteName(qualifiedName);
    Objects.requireNonNull(typeName, "typeName");
    Objects.requireNonNull(resolvedTypeName, "resolvedTypeName");
    Objects.requireNonNull(constantValue, "constantValue");
    Objects.requireNonNull(repositoryId, "repositoryId");
    repositoryId.ifPresent(value -> requireNonBlank(value, "repositoryId"));
    Objects.requireNonNull(span, "span");
  }

  /** Creates a symbol without repository ID metadata. */
  public IdlSymbol(
      IdlSymbolKind kind,
      String name,
      String qualifiedName,
      Optional<String> typeName,
      Optional<String> resolvedTypeName,
      Optional<IdlConstantValue> constantValue,
      SourceSpan span) {
    this(
        kind,
        name,
        qualifiedName,
        typeName,
        resolvedTypeName,
        constantValue,
        Optional.empty(),
        span);
  }

  private static String requireAbsoluteName(String value) {
    value = requireNonBlank(value, "qualifiedName");
    if (!value.startsWith("::")) {
      throw new IllegalArgumentException("qualifiedName must be an absolute IDL name");
    }
    return value;
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
