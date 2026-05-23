package io.github.mundanej.mjo.nativeimage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NativeImageToolchainDiscoveryTest {

  @TempDir private Path temporaryDirectory;

  @Test
  void explicitNativeImageWinsOverOtherCandidates() throws IOException {
    Path explicit = nativeImageAt(temporaryDirectory.resolve("explicit/bin"));
    Path javaHome = temporaryDirectory.resolve("java-home");
    nativeImageAt(javaHome.resolve("bin"));

    NativeImageToolchain toolchain =
        discover(
            Map.of("NATIVE_IMAGE", explicit.toString(), "JAVA_HOME", javaHome.toString()),
            List.of());

    assertEquals(explicit, toolchain.executable());
    assertEquals(NativeImageToolchain.Source.NATIVE_IMAGE, toolchain.source());
  }

  @Test
  void javaHomeWinsOverSdkmanAndPathCandidates() throws IOException {
    Path javaHome = temporaryDirectory.resolve("java-home");
    Path javaHomeNativeImage = nativeImageAt(javaHome.resolve("bin"));
    Path sdkman = temporaryDirectory.resolve("sdkman");
    nativeImageAt(sdkman.resolve("java/21.0.2-graalce/bin"));
    Path pathDirectory = temporaryDirectory.resolve("path-bin");
    nativeImageAt(pathDirectory);

    NativeImageToolchain toolchain =
        discover(
            Map.of("JAVA_HOME", javaHome.toString(), "SDKMAN_CANDIDATES_DIR", sdkman.toString()),
            List.of(pathDirectory));

    assertEquals(javaHomeNativeImage, toolchain.executable());
    assertEquals(NativeImageToolchain.Source.JAVA_HOME, toolchain.source());
  }

  @Test
  void sdkmanCurrentWinsOverSortedSdkmanCandidates() throws IOException {
    Path sdkman = temporaryDirectory.resolve("sdkman");
    nativeImageAt(sdkman.resolve("java/21.0.2-graalce/bin"));
    Path current = nativeImageAt(sdkman.resolve("java/current/bin"));

    NativeImageToolchain toolchain =
        discover(Map.of("SDKMAN_CANDIDATES_DIR", sdkman.toString()), List.of());

    assertEquals(current, toolchain.executable());
    assertEquals(NativeImageToolchain.Source.SDKMAN, toolchain.source());
  }

  @Test
  void sdkmanCandidatesUseSortedLastCandidateWhenCurrentIsAbsent() throws IOException {
    Path sdkman = temporaryDirectory.resolve("sdkman");
    nativeImageAt(sdkman.resolve("java/21.0.1-graalce/bin"));
    Path expected = nativeImageAt(sdkman.resolve("java/21.0.2-graalce/bin"));
    nativeImageAt(sdkman.resolve("java/17.0.10-graalce/bin"));

    NativeImageToolchain toolchain =
        discover(Map.of("SDKMAN_CANDIDATES_DIR", sdkman.toString()), List.of());

    assertEquals(expected, toolchain.executable());
    assertEquals(NativeImageToolchain.Source.SDKMAN, toolchain.source());
  }

  @Test
  void pathCandidatePrecedesBareCommandFallback() throws IOException {
    Path pathDirectory = temporaryDirectory.resolve("path-bin");
    Path nativeImage = nativeImageAt(pathDirectory);

    NativeImageToolchain toolchain = discover(Map.of(), List.of(pathDirectory));

    assertEquals(nativeImage, toolchain.executable());
    assertEquals(NativeImageToolchain.Source.PATH, toolchain.source());
  }

  @Test
  void bareCommandFallbackIsDeterministicWhenNoCandidateExists() {
    NativeImageToolchain toolchain =
        discover(Map.of("HOME", temporaryDirectory.toString()), List.of());

    assertEquals(Path.of("native-image"), toolchain.executable());
    assertEquals(NativeImageToolchain.Source.BARE_COMMAND, toolchain.source());
  }

  @Test
  void environmentDefensivelyCopiesInputsAndExposesImmutableViews() {
    Map<String, String> variables = new HashMap<>();
    variables.put("NATIVE_IMAGE", "native-image-a");
    List<Path> pathDirectories = new ArrayList<>(List.of(temporaryDirectory.resolve("bin-a")));
    NativeImageEnvironment environment = new NativeImageEnvironment(variables, pathDirectories);

    variables.put("NATIVE_IMAGE", "native-image-b");
    pathDirectories.add(temporaryDirectory.resolve("bin-b"));

    assertEquals("native-image-a", environment.variables().get("NATIVE_IMAGE"));
    assertEquals(List.of(temporaryDirectory.resolve("bin-a")), environment.pathDirectories());
    assertThrows(
        UnsupportedOperationException.class, () -> environment.variables().put("OTHER", "value"));
    assertThrows(UnsupportedOperationException.class, () -> environment.pathDirectories().clear());
  }

  @Test
  void explicitMissingNativeImageFailsDeterministically() {
    Path missing = temporaryDirectory.resolve("missing-native-image");

    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () -> discover(Map.of("NATIVE_IMAGE", missing.toString()), List.of()));

    assertEquals("NATIVE_IMAGE does not point to native-image: " + missing, failure.getMessage());
  }

  private NativeImageToolchain discover(Map<String, String> variables, List<Path> pathDirectories) {
    return new NativeImageToolchainDiscovery()
        .discover(new NativeImageEnvironment(variables, pathDirectories));
  }

  private static Path nativeImageAt(Path directory) throws IOException {
    Files.createDirectories(directory);
    Path nativeImage = directory.resolve("native-image");
    Files.writeString(nativeImage, "fixture");
    return nativeImage;
  }
}
