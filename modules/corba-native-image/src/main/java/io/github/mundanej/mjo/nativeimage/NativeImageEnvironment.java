package io.github.mundanej.mjo.nativeimage;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable environment view used for deterministic Native Image tool discovery. */
public record NativeImageEnvironment(Map<String, String> variables, List<Path> pathDirectories) {

  /** Creates an environment with immutable storage. */
  public NativeImageEnvironment {
    variables = Map.copyOf(Objects.requireNonNull(variables, "variables"));
    pathDirectories = List.copyOf(Objects.requireNonNull(pathDirectories, "pathDirectories"));
  }

  /** Captures the current process environment without launching any process. */
  public static NativeImageEnvironment system() {
    Map<String, String> variables = System.getenv();
    String path = variables.getOrDefault("PATH", "");
    List<Path> paths =
        path.isBlank()
            ? List.of()
            : Arrays.stream(path.split(java.io.File.pathSeparator))
                .filter(value -> !value.isBlank())
                .map(Path::of)
                .toList();
    return new NativeImageEnvironment(variables, paths);
  }

  String variable(String name) {
    return variables.getOrDefault(name, "");
  }
}
