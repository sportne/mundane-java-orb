package io.github.mundanej.mjo.codegen;

import java.util.Objects;

/**
 * Immutable generated Java source file.
 *
 * @param packageName Java package name, or empty for the default package
 * @param simpleName Java top-level type name
 * @param sourceText complete Java source text
 */
public record GeneratedJavaSource(String packageName, String simpleName, String sourceText) {

  /** Creates a validated generated source value. */
  public GeneratedJavaSource {
    Objects.requireNonNull(packageName, "packageName");
    simpleName = requireNonBlank(simpleName, "simpleName");
    sourceText = requireNonBlank(sourceText, "sourceText");
  }

  /** Returns the deterministic relative source path. */
  public String sourcePath() {
    String fileName = simpleName + ".java";
    return packageName.isEmpty() ? fileName : packageName.replace('.', '/') + "/" + fileName;
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
