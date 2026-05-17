package io.github.mundanej.mjo.idl.semantics;

import io.github.mundanej.mjo.idl.ast.IdlTranslationUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable semantic model for one IDL translation unit.
 *
 * @param translationUnit source AST analyzed to produce the model
 * @param symbols deterministic semantic symbols in encounter order
 */
public record IdlSemanticModel(IdlTranslationUnit translationUnit, List<IdlSymbol> symbols) {

  /** Creates an immutable semantic model. */
  public IdlSemanticModel {
    Objects.requireNonNull(translationUnit, "translationUnit");
    symbols = List.copyOf(Objects.requireNonNull(symbols, "symbols"));
  }

  /** Finds a symbol by absolute IDL name. A missing leading {@code ::} is added for convenience. */
  public Optional<IdlSymbol> findSymbol(String qualifiedName) {
    String normalized = normalizeQualifiedName(qualifiedName);
    return symbols.stream().filter(symbol -> symbol.qualifiedName().equals(normalized)).findFirst();
  }

  /** Returns all symbols of a given kind in model order. */
  public List<IdlSymbol> symbols(IdlSymbolKind kind) {
    Objects.requireNonNull(kind, "kind");
    return symbols.stream().filter(symbol -> symbol.kind() == kind).toList();
  }

  private static String normalizeQualifiedName(String qualifiedName) {
    Objects.requireNonNull(qualifiedName, "qualifiedName");
    if (qualifiedName.isBlank()) {
      throw new IllegalArgumentException("qualifiedName must not be blank");
    }
    return qualifiedName.startsWith("::") ? qualifiedName : "::" + qualifiedName;
  }
}
