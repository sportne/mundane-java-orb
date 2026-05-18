package io.github.mundanej.mjo.nativeimage;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Deterministic native-image command model for one smoke target. */
public record NativeImageCommand(
    NativeImageToolchain toolchain,
    NativeImageTarget target,
    List<Path> classpathEntries,
    Path outputDirectory) {

  /** Creates a command with immutable classpath storage. */
  public NativeImageCommand {
    Objects.requireNonNull(toolchain, "toolchain");
    Objects.requireNonNull(target, "target");
    classpathEntries = List.copyOf(Objects.requireNonNull(classpathEntries, "classpathEntries"));
    Objects.requireNonNull(outputDirectory, "outputDirectory");
  }

  /** Returns the output binary path. */
  public Path outputBinary() {
    return outputDirectory.resolve(target.binaryName());
  }

  /** Returns native-image arguments in deterministic order. */
  public List<String> compileArguments() {
    List<String> arguments = new ArrayList<>();
    arguments.add("--no-fallback");
    arguments.add("-H:+ReportExceptionStackTraces");
    arguments.add("-o");
    arguments.add(outputBinary().toString());
    arguments.add("-cp");
    arguments.add(classpath());
    arguments.add(target.mainClass());
    return List.copyOf(arguments);
  }

  /** Returns a display-safe command line without shell interpretation. */
  public List<String> commandLine() {
    List<String> command = new ArrayList<>();
    command.add(toolchain.executable().toString());
    command.addAll(compileArguments());
    return List.copyOf(command);
  }

  private String classpath() {
    return String.join(File.pathSeparator, classpathEntries.stream().map(Path::toString).toList());
  }
}
