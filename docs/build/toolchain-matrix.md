# Toolchain Matrix

| Mode | JDK | Required |
|---|---|---|
| JVM | OpenJDK 21 | yes |
| JVM | OpenJDK 25 | yes |
| JVM | GraalVM JDK 21 | yes |
| JVM | GraalVM JDK 25 | yes |
| Native Image | GraalVM JDK 21 | yes |
| Native Image | GraalVM JDK 25 | yes |

Production bytecode target: Java release 21.

## GraalVM Native Image discovery

Native Image validation must not assume GraalVM is on the ambient shell `PATH`.
When running native checks locally or from an agent session:

1. Prefer an explicit `NATIVE_IMAGE` environment variable when the caller
   supplies one.
2. Otherwise prefer `JAVA_HOME/bin/native-image` when `JAVA_HOME` points at a
   GraalVM JDK.
3. Otherwise inspect SDKMAN candidates under `$SDKMAN_CANDIDATES_DIR/java` or
   `$HOME/.sdkman/candidates/java` and select a GraalVM candidate matching the
   required Java lane.
4. Use a bare `native-image` command only as a final fallback.

Native smoke commands should set `JAVA_HOME` and prepend `$JAVA_HOME/bin` to
`PATH` once a SDKMAN GraalVM candidate is found.
