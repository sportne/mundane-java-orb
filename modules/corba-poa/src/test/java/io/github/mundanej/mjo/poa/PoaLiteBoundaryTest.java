package io.github.mundanej.mjo.poa;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Boundary tests for POA production code. */
@Tag("unit")
final class PoaLiteBoundaryTest {

  @Test
  void productionPoaSourceDoesNotReferenceForbiddenRuntimeMechanisms() throws IOException {
    Path productionRoot = Path.of("src/main/java/io/github/mundanej/mjo/poa");
    String source = readProductionSource(productionRoot);

    assertFalse(source.contains("io.github.mundanej.mjo.giop"));
    assertFalse(source.contains("io.github.mundanej.mjo.iiop"));
    assertFalse(source.contains("io.github.mundanej.mjo.services"));
    assertFalse(source.contains("java.lang.reflect"));
    assertFalse(source.contains("java.io.ObjectInput"));
    assertFalse(source.contains("java.io.ObjectOutput"));
    assertFalse(source.contains("Proxy.newProxyInstance"));
  }

  private static String readProductionSource(Path productionRoot) throws IOException {
    StringBuilder source = new StringBuilder();
    try (Stream<Path> paths = Files.walk(productionRoot)) {
      for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
        source.append(Files.readString(path)).append('\n');
      }
    }
    return source.toString();
  }
}
