package io.github.mundanej.mjo.rmi.iiop;

import java.util.Objects;

/**
 * Deterministic generated Java binding source for one RMI Java-to-IDL model type.
 *
 * @param packageName Java package name, or empty for the default package
 * @param simpleName generated top-level Java type name
 * @param sourceText complete Java source text
 */
public record RmiGeneratedJavaBindingSource(
    String packageName, String simpleName, String sourceText) {

  /** Creates a validated generated binding source value. */
  public RmiGeneratedJavaBindingSource {
    Objects.requireNonNull(packageName, "packageName");
    simpleName = requireNonBlank(simpleName, "simpleName");
    sourceText = requireNonBlank(sourceText, "sourceText");
    if (!sourceText.endsWith("\n")) {
      throw new IllegalArgumentException("sourceText must end with a newline");
    }
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
