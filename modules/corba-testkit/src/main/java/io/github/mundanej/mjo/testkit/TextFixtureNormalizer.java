package io.github.mundanej.mjo.testkit;

import java.util.Objects;

/** Normalizes text fixtures for deterministic golden comparisons. */
public final class TextFixtureNormalizer {

  private static final char UTF8_BOM = '\ufeff';

  private TextFixtureNormalizer() {}

  /** Removes one leading UTF-8 BOM and normalizes CRLF/CR line endings to LF. */
  public static String normalize(String text) {
    Objects.requireNonNull(text, "text");
    String withoutBom = text.isEmpty() || text.charAt(0) != UTF8_BOM ? text : text.substring(1);
    return withoutBom.replace("\r\n", "\n").replace('\r', '\n');
  }
}
