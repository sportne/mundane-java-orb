package io.github.mundanej.mjo.nativeimage;

import java.nio.file.Path;
import java.util.Objects;

/** Discovered native-image executable and its source. */
public record NativeImageToolchain(Path executable, Source source) {

  /** Source used to locate the executable. */
  public enum Source {
    /** Explicit NATIVE_IMAGE environment variable. */
    NATIVE_IMAGE,
    /** JAVA_HOME/bin/native-image. */
    JAVA_HOME,
    /** SDKMAN Java candidate. */
    SDKMAN,
    /** Native image found on PATH. */
    PATH,
    /** Bare native-image command fallback. */
    BARE_COMMAND
  }

  /** Creates a validated toolchain value. */
  public NativeImageToolchain {
    Objects.requireNonNull(executable, "executable");
    Objects.requireNonNull(source, "source");
  }
}
