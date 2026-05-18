# corba-native-image

Native Image validation harnesses, smoke binaries, and metadata audits for the
local G6 ORB runtime.

## Current status

G6-910 adds verification-only Native Image coverage. This module does not add
new CORBA runtime behavior; it compiles and runs representative entry points
from existing modules as GraalVM Native Image executables.

## Toolchain discovery

The Java harness and Gradle tasks use the same precedence:

1. `NATIVE_IMAGE`
2. `JAVA_HOME/bin/native-image`
3. SDKMAN Java candidates under `$SDKMAN_CANDIDATES_DIR/java` or
   `$HOME/.sdkman/candidates/java`
4. `native-image` on `PATH`

The smoke tasks pass `--no-fallback` and `-H:+ReportExceptionStackTraces`.

## Smoke binaries

- `idljValidate`: validation-only IDL CLI over compact valid and invalid IDL.
- `generatedClient`: generated-style local client invocation through
  `LocalOrb`.
- `generatedServer`: generated-style servant/dispatcher invocation through
  `LocalOrb`.
- `namingServer`: local `NameService` install, bind, resolve, and
  `corbaname:rir:` resolution.
- `iorDiagnostics`: deterministic `corbaloc`, `corbaname`, and stringified IOR
  parsing.
- `interopReport`: structured G6 interop report JSON serialization and parsing.

Run all binaries with:

```bash
./gradlew :modules:corba-native-image:nativeImageBinariesSmoke
```

## Native Image boundaries

This slice requires no reflection, dynamic proxy, serialization, service-loader,
classpath-scanning, runtime bytecode-generation, or internal JDK metadata. Future
metadata must be checked in as reviewed source and covered by the matrix in
`docs/verification/native-image-matrix.md`.
