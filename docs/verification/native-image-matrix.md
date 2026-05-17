# Native Image Matrix

## Toolchains

- GraalVM JDK 21 Native Image.
- GraalVM JDK 25 Native Image.

Native Image checks should discover SDKMAN-installed GraalVM candidates before
falling back to a bare `native-image` command on `PATH`. Prefer explicit
`NATIVE_IMAGE`, then `JAVA_HOME/bin/native-image`, then SDKMAN candidates under
`$SDKMAN_CANDIDATES_DIR/java` or `$HOME/.sdkman/candidates/java`. When a SDKMAN
candidate is used, run Gradle with `JAVA_HOME` set to that candidate and
`$JAVA_HOME/bin` prepended to `PATH`.

## Required native binaries

- CDR primitive test binary.
- `corba-idlj`.
- generated client sample.
- generated server sample.
- naming server.
- IOR diagnostic tool.
- selected interop clients/servers.

## Required test levels

- smoke;
- integration;
- interop;
- startup/shutdown;
- class-initialization audit;
- reflection metadata audit.

## Current G6 native checks

- `:modules:corba-cdr:nativeCdrSmoke` builds and executes a GraalVM Native
  Image smoke executable for the CDR primitive reader and writer API.
- `:modules:corba-idl-semantics:nativeIdlSemanticsSmoke` builds and executes a
  GraalVM Native Image smoke executable for parser-to-semantics behavior over a
  compact IDL fixture.
- `:modules:corba-idlj-cli:nativeIdljValidateSmoke` builds and executes a
  GraalVM Native Image smoke executable for the validation-only `corba-idlj`
  command path over compact valid and invalid IDL fixtures.
