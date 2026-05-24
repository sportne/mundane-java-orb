package io.github.mundanej.mjo.dynamic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Boundary tests for native-friendly local dynamic behavior. */
@Tag("unit")
final class DynamicBoundaryTest {

  @Test
  void mainSourcesDoNotIntroduceForbiddenDynamicOrTransportMechanisms() throws IOException {
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
            "io.github.mundanej.mjo.giop",
            "org.omg.DynamicAny",
            "org.omg.CORBA.Request");

    assertEquals(List.of(), forbiddenTokens.stream().filter(source::contains).toList());
  }

  private static String productionSource(String root) throws IOException {
    try (Stream<Path> paths = Files.walk(Path.of(root))) {
      return paths
          .filter(path -> path.toString().endsWith(".java"))
          .map(DynamicBoundaryTest::readString)
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
