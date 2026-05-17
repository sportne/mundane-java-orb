package io.github.mundanej.mjo.idl.preprocessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Include resolver that reads sources from configured include roots.
 *
 * <p>Requested include names must be relative paths. Absolute paths, parent traversal, backslash
 * separators, and normalized paths that escape an include root are rejected before any file read is
 * attempted.
 */
public final class PathIdlIncludeResolver implements IdlIncludeResolver {

  private final List<Path> includeRoots;

  /** Creates a resolver with validated include roots. */
  public PathIdlIncludeResolver(List<Path> includeRoots) {
    Objects.requireNonNull(includeRoots, "includeRoots");
    this.includeRoots =
        includeRoots.stream()
            .map(root -> Objects.requireNonNull(root, "include root"))
            .map(Path::toAbsolutePath)
            .map(Path::normalize)
            .toList();
  }

  /** Creates a resolver with validated include roots. */
  public static PathIdlIncludeResolver of(Path... includeRoots) {
    Objects.requireNonNull(includeRoots, "includeRoots");
    return new PathIdlIncludeResolver(Arrays.asList(includeRoots));
  }

  /** Returns immutable include roots used by this resolver. */
  public List<Path> includeRoots() {
    return List.copyOf(includeRoots);
  }

  @Override
  public Optional<IdlSource> resolve(IdlIncludeRequest request) throws IOException {
    Objects.requireNonNull(request, "request");
    Path relative = safeRelativePath(request.includeName());
    for (Path root : includeRoots) {
      Path candidate = root.resolve(relative).normalize();
      if (!candidate.startsWith(root)) {
        throw new SecurityException(
            "Include path escapes configured root: " + request.includeName());
      }
      if (Files.isRegularFile(candidate)) {
        return Optional.of(
            new IdlSource(
                candidate.toString(), Files.readString(candidate, StandardCharsets.UTF_8)));
      }
    }
    return Optional.empty();
  }

  private static Path safeRelativePath(String includeName) {
    if (includeName.indexOf('\\') >= 0) {
      throw new SecurityException("Include path must use forward slash separators: " + includeName);
    }
    try {
      Path path = Path.of(includeName);
      if (path.isAbsolute()) {
        throw new SecurityException("Include path must be relative: " + includeName);
      }
      Path normalized = path.normalize();
      if (normalized.toString().isBlank() || startsWithParentTraversal(normalized)) {
        throw new SecurityException("Include path must not traverse parents: " + includeName);
      }
      return normalized;
    } catch (InvalidPathException exception) {
      throw new SecurityException("Include path is not valid: " + includeName, exception);
    }
  }

  private static boolean startsWithParentTraversal(Path path) {
    return path.getNameCount() > 0 && "..".equals(path.getName(0).toString());
  }
}
