package io.github.mundanej.mjo.naming.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Boundary tests for the local Naming Service slice. */
@Tag("unit")
final class NamingBoundaryTest {

  @Test
  void namingProductionSourcesDoNotIntroduceDynamicDiscoveryTransportOrLegacyApis()
      throws IOException {
    Path repositoryRoot = findRepositoryRoot();
    String source =
        productionSource(repositoryRoot.resolve("modules/corba-naming-api/src/main/java"))
            + productionSource(repositoryRoot.resolve("modules/corba-naming-server/src/main/java"));
    List<String> forbiddenTokens =
        List.of(
            "java.lang.reflect",
            "java.lang.ClassLoader",
            "java.lang.reflect.Proxy",
            "ObjectInputStream",
            "ObjectOutputStream",
            "java.io.Serializable",
            "java.lang.invoke",
            "java.util.ServiceLoader",
            "Class.forName",
            "io.github.mundanej.mjo.giop",
            "io.github.mundanej.mjo.iiop",
            "org.omg.CosNaming",
            "corba-services-core",
            "peer interop");

    assertEquals(List.of(), forbiddenTokens.stream().filter(source::contains).toList());
  }

  private static String productionSource(Path root) throws IOException {
    try (Stream<Path> paths = Files.walk(root)) {
      return paths
          .filter(path -> path.toString().endsWith(".java"))
          .map(NamingBoundaryTest::readString)
          .reduce("", String::concat);
    }
  }

  private static String readString(Path path) {
    try {
      return Files.readString(path, StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new AssertionError("failed to read source: " + path, exception);
    }
  }

  private static Path findRepositoryRoot() {
    Path directory = Path.of("").toAbsolutePath().normalize();
    while (directory != null) {
      if (Files.isRegularFile(directory.resolve("AGENT.md"))
          && Files.isDirectory(directory.resolve("modules"))) {
        return directory;
      }
      directory = directory.getParent();
    }
    throw new IllegalStateException("Could not locate repository root from test working directory");
  }
}
