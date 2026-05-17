package io.github.mundanej.mjo.testkit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;

/** Path-rooted fixture loader with traversal-safe relative lookup. */
public final class FixtureSet {

  private final Path root;

  /** Creates a fixture set rooted at an existing directory. */
  public FixtureSet(Path root) {
    Path normalizedRoot = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    if (!Files.isDirectory(normalizedRoot)) {
      throw new IllegalArgumentException("fixture root must be an existing directory: " + root);
    }
    this.root = normalizedRoot;
  }

  /** Returns the normalized absolute fixture root. */
  public Path root() {
    return root;
  }

  /** Resolves a safe relative fixture path under the root. */
  public Path resolve(String relativePath) {
    Path relative = requireSafeRelativePath(relativePath);
    Path resolved = root.resolve(relative).normalize();
    if (!resolved.startsWith(root)) {
      throw new IllegalArgumentException("fixture path escapes root: " + relativePath);
    }
    return resolved;
  }

  /** Reads a fixture as raw UTF-8 text. */
  public String readUtf8(String relativePath) throws IOException {
    return Files.readString(requireRegularFixture(relativePath), StandardCharsets.UTF_8);
  }

  /** Reads a fixture as UTF-8 text and applies the testkit text normalizer. */
  public String readNormalizedUtf8(String relativePath) throws IOException {
    return TextFixtureNormalizer.normalize(readUtf8(relativePath));
  }

  /** Reads a fixture as bytes. */
  public byte[] readBytes(String relativePath) throws IOException {
    return Files.readAllBytes(requireRegularFixture(relativePath));
  }

  static Path requireSafeRelativePath(String relativePath) {
    Objects.requireNonNull(relativePath, "relativePath");
    if (relativePath.isBlank()) {
      throw new IllegalArgumentException("fixture path must not be blank");
    }
    Path rawRelative = Path.of(relativePath);
    if (rawRelative.isAbsolute()) {
      throw new IllegalArgumentException("fixture path must be relative: " + relativePath);
    }
    for (Path segment : rawRelative) {
      if ("..".equals(segment.toString())) {
        throw new IllegalArgumentException("fixture path must not contain '..': " + relativePath);
      }
    }
    return rawRelative.normalize();
  }

  private Path requireRegularFixture(String relativePath) throws IOException {
    Path resolved = resolve(relativePath);
    if (!Files.isRegularFile(resolved)) {
      throw new NoSuchFileException(resolved.toString());
    }
    return resolved;
  }
}
