package io.github.mundanej.mjo.repositoryid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RepositoryIdVersion}. */
@Tag("unit")
final class RepositoryIdVersionTest {

  @Test
  void createsNonnegativeMajorMinorVersion() {
    RepositoryIdVersion version = new RepositoryIdVersion(2, 10);

    assertEquals(2, version.major());
    assertEquals(10, version.minor());
    assertEquals("2.10", version.toString());
  }

  @Test
  void parsesAndNormalizesDecimalComponents() {
    assertEquals(new RepositoryIdVersion(1, 2), RepositoryIdVersion.parse("01.002"));
  }

  @Test
  void acceptsMaximumLongVersionComponents() {
    RepositoryIdVersion version = RepositoryIdVersion.parse(Long.MAX_VALUE + "." + Long.MAX_VALUE);

    assertEquals(Long.MAX_VALUE, version.major());
    assertEquals(Long.MAX_VALUE, version.minor());
    assertEquals(Long.MAX_VALUE + "." + Long.MAX_VALUE, version.toString());
  }

  @Test
  void rejectsInvalidVersionValues() {
    assertThrows(NullPointerException.class, () -> RepositoryIdVersion.parse(null));
    assertThrows(RepositoryIdException.class, () -> new RepositoryIdVersion(-1, 0));
    assertThrows(RepositoryIdException.class, () -> new RepositoryIdVersion(1, -1));
    assertThrows(RepositoryIdException.class, () -> RepositoryIdVersion.parse("1"));
    assertThrows(RepositoryIdException.class, () -> RepositoryIdVersion.parse("1."));
    assertThrows(RepositoryIdException.class, () -> RepositoryIdVersion.parse(".1"));
    assertThrows(RepositoryIdException.class, () -> RepositoryIdVersion.parse("1.2.3"));
    assertThrows(RepositoryIdException.class, () -> RepositoryIdVersion.parse("1.-2"));
    assertThrows(
        RepositoryIdException.class, () -> RepositoryIdVersion.parse("999999999999999999999.0"));
    assertThrows(
        RepositoryIdException.class, () -> RepositoryIdVersion.parse("9223372036854775808.0"));
  }
}
