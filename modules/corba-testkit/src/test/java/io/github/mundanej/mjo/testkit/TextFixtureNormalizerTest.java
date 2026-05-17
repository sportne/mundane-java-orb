package io.github.mundanej.mjo.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TextFixtureNormalizer}. */
@Tag("unit")
final class TextFixtureNormalizerTest {

  @Test
  void removesOneLeadingUtf8Bom() {
    assertEquals("module x {};", TextFixtureNormalizer.normalize("\ufeffmodule x {};"));
    assertEquals("\ufeffmodule x {};", TextFixtureNormalizer.normalize("\ufeff\ufeffmodule x {};"));
  }

  @Test
  void normalizesLineEndingsWithoutTrimmingContent() {
    String input = "  first\r\nsecond\rthird\n  ";

    assertEquals("  first\nsecond\nthird\n  ", TextFixtureNormalizer.normalize(input));
  }

  @Test
  void rejectsNullText() {
    assertThrows(NullPointerException.class, () -> TextFixtureNormalizer.normalize(null));
  }
}
