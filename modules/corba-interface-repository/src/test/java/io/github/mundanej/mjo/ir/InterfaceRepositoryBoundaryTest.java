package io.github.mundanej.mjo.ir;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Boundary tests for closed-world static Interface Repository behavior. */
@Tag("unit")
final class InterfaceRepositoryBoundaryTest {

  @Test
  void mainSourcesDoNotIntroduceDynamicDiscoveryOrTransportMechanisms() throws IOException {
    String source = productionSource("src/main/java");
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
            "Files.walk",
            "Class.forName",
            "io.github.mundanej.mjo.giop",
            "io.github.mundanej.mjo.iiop",
            "io.github.mundanej.mjo.orb",
            "org.omg.CORBA.Repository");

    assertEquals(List.of(), forbiddenTokens.stream().filter(source::contains).toList());
  }

  private static String productionSource(String root) throws IOException {
    try (Stream<Path> paths = Files.walk(Path.of(root))) {
      return paths
          .filter(path -> path.toString().endsWith(".java"))
          .map(InterfaceRepositoryBoundaryTest::readString)
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
}
