package io.github.mundanej.mjo.nativeimage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class NativeImageCommandTest {

  @Test
  void commandArgumentsAreDeterministic() {
    NativeImageToolchain toolchain =
        new NativeImageToolchain(
            Path.of("graal", "bin", "native-image"), NativeImageToolchain.Source.JAVA_HOME);
    NativeImageTarget target =
        new NativeImageTarget("sample", "example.Main", "sample-native-smoke");

    NativeImageCommand command =
        new NativeImageCommand(
            toolchain,
            target,
            List.of(Path.of("a.jar"), Path.of("b.jar")),
            Path.of("build/native"));

    assertEquals(Path.of("build/native/sample-native-smoke"), command.outputBinary());
    assertEquals(
        List.of(
            "--no-fallback",
            "-H:+ReportExceptionStackTraces",
            "-o",
            "build/native/sample-native-smoke",
            "-cp",
            "a.jar" + File.pathSeparator + "b.jar",
            "example.Main"),
        command.compileArguments());
    assertEquals(
        Path.of("graal", "bin", "native-image").toString(), command.commandLine().getFirst());
  }

  @Test
  void targetRejectsBlankFields() {
    assertThrows(
        IllegalArgumentException.class, () -> new NativeImageTarget("", "example.Main", "x"));
    assertThrows(IllegalArgumentException.class, () -> new NativeImageTarget("sample", " ", "x"));
    assertThrows(
        IllegalArgumentException.class, () -> new NativeImageTarget("sample", "example.Main", ""));
  }

  @Test
  void g6TargetCatalogIsStable() {
    assertEquals(
        List.of(
            "idljValidate",
            "generatedClient",
            "generatedServer",
            "namingServer",
            "iorDiagnostics",
            "interopReport"),
        NativeImageTargets.g6Targets().stream().map(NativeImageTarget::name).toList());
  }
}
