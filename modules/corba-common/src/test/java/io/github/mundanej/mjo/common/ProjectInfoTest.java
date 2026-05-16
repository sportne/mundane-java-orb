package io.github.mundanej.mjo.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for the repository scaffold metadata. */
@Tag("unit")
final class ProjectInfoTest {

  @Test
  void projectNameIdentifiesTheScaffold() {
    assertEquals("mundane Java ORB", ProjectInfo.PROJECT_NAME);
  }
}
