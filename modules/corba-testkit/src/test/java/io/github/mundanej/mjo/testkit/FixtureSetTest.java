package io.github.mundanej.mjo.testkit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link FixtureSet}. */
@Tag("unit")
final class FixtureSetTest {

  @TempDir Path temporaryDirectory;

  @Test
  void resolvesSafeRelativePathsUnderRoot() throws IOException {
    Path nested = Files.createDirectories(temporaryDirectory.resolve("idl/basic"));
    Files.writeString(nested.resolve("hello.idl"), "module hello {};", StandardCharsets.UTF_8);
    FixtureSet fixtures = new FixtureSet(temporaryDirectory);

    Path resolved = fixtures.resolve("idl/basic/hello.idl");

    assertEquals(temporaryDirectory.toAbsolutePath().normalize(), fixtures.root());
    assertTrue(resolved.startsWith(fixtures.root()));
    assertEquals("module hello {};", fixtures.readUtf8("idl/basic/hello.idl"));
  }

  @Test
  void readsBytesFromFixture() throws IOException {
    Files.write(temporaryDirectory.resolve("wire.bin"), new byte[] {0x00, 0x10, (byte) 0xFF});
    FixtureSet fixtures = new FixtureSet(temporaryDirectory);

    assertArrayEquals(new byte[] {0x00, 0x10, (byte) 0xFF}, fixtures.readBytes("wire.bin"));
  }

  @Test
  void readsNormalizedUtf8Fixture() throws IOException {
    Files.writeString(
        temporaryDirectory.resolve("text.txt"), "\ufeffa\r\nb\rc", StandardCharsets.UTF_8);
    FixtureSet fixtures = new FixtureSet(temporaryDirectory);

    assertEquals("a\nb\nc", fixtures.readNormalizedUtf8("text.txt"));
  }

  @Test
  void rejectsUnsafeFixturePaths() {
    FixtureSet fixtures = new FixtureSet(temporaryDirectory);

    assertThrows(IllegalArgumentException.class, () -> fixtures.resolve(""));
    assertThrows(IllegalArgumentException.class, () -> fixtures.resolve(" "));
    assertThrows(IllegalArgumentException.class, () -> fixtures.resolve("../outside.idl"));
    assertThrows(IllegalArgumentException.class, () -> fixtures.resolve("a/../outside.idl"));
    assertThrows(
        IllegalArgumentException.class,
        () -> fixtures.resolve(temporaryDirectory.resolve("absolute.idl").toString()));
  }

  @Test
  void rejectsMissingRootAndMissingFixtureFiles() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new FixtureSet(temporaryDirectory.resolve("missing")));

    FixtureSet fixtures = new FixtureSet(temporaryDirectory);

    assertThrows(NoSuchFileException.class, () -> fixtures.readUtf8("missing.idl"));
    assertThrows(NoSuchFileException.class, () -> fixtures.readBytes("missing.bin"));
  }
}
