package io.github.mundanej.mjo.nativeimage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

/** Deterministic GraalVM Native Image executable discovery. */
public final class NativeImageToolchainDiscovery {

  private static final String EXECUTABLE_NAME = "native-image";

  /** Discovers a native-image executable from the current process environment. */
  public NativeImageToolchain discover() {
    return discover(NativeImageEnvironment.system());
  }

  /** Discovers a native-image executable from a supplied environment view. */
  public NativeImageToolchain discover(NativeImageEnvironment environment) {
    Objects.requireNonNull(environment, "environment");
    String explicit = environment.variable("NATIVE_IMAGE");
    if (!explicit.isBlank()) {
      Path executable = Path.of(explicit);
      requireRegularFile(executable, "NATIVE_IMAGE");
      return new NativeImageToolchain(executable, NativeImageToolchain.Source.NATIVE_IMAGE);
    }

    String javaHome = environment.variable("JAVA_HOME");
    if (!javaHome.isBlank()) {
      Path executable = Path.of(javaHome, "bin", EXECUTABLE_NAME);
      if (Files.isRegularFile(executable)) {
        return new NativeImageToolchain(executable, NativeImageToolchain.Source.JAVA_HOME);
      }
    }

    NativeImageToolchain sdkman = discoverSdkman(environment);
    if (sdkman != null) {
      return sdkman;
    }

    for (Path directory : environment.pathDirectories()) {
      Path executable = directory.resolve(EXECUTABLE_NAME);
      if (Files.isRegularFile(executable)) {
        return new NativeImageToolchain(executable, NativeImageToolchain.Source.PATH);
      }
    }

    return new NativeImageToolchain(
        Path.of(EXECUTABLE_NAME), NativeImageToolchain.Source.BARE_COMMAND);
  }

  private static NativeImageToolchain discoverSdkman(NativeImageEnvironment environment) {
    Path javaCandidates = sdkmanJavaCandidates(environment);
    Path current = javaCandidates.resolve("current/bin").resolve(EXECUTABLE_NAME);
    if (Files.isRegularFile(current)) {
      return new NativeImageToolchain(current, NativeImageToolchain.Source.SDKMAN);
    }
    if (!Files.isDirectory(javaCandidates)) {
      return null;
    }
    try (var stream = Files.list(javaCandidates)) {
      return stream
          .map(path -> path.resolve("bin").resolve(EXECUTABLE_NAME))
          .filter(Files::isRegularFile)
          .sorted(Comparator.comparing(Path::toString))
          .reduce((first, second) -> second)
          .map(path -> new NativeImageToolchain(path, NativeImageToolchain.Source.SDKMAN))
          .orElse(null);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException(
          "Could not inspect SDKMAN candidates: " + javaCandidates, exception);
    }
  }

  private static Path sdkmanJavaCandidates(NativeImageEnvironment environment) {
    String candidates = environment.variable("SDKMAN_CANDIDATES_DIR");
    if (!candidates.isBlank()) {
      return Path.of(candidates, "java");
    }
    String home = environment.variable("HOME");
    if (!home.isBlank()) {
      return Path.of(home, ".sdkman", "candidates", "java");
    }
    return Path.of(".sdkman-unavailable");
  }

  private static void requireRegularFile(Path executable, String sourceName) {
    if (!Files.isRegularFile(executable)) {
      throw new IllegalArgumentException(
          sourceName + " does not point to native-image: " + executable);
    }
  }
}
