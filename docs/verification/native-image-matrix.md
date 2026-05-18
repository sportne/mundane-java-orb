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

- `:modules:corba-native-image:nativeImageBinariesSmoke` builds and executes
  the G6-910 aggregate Native Image smoke suite:
  - `idljValidate` validates compact valid and invalid IDL through `IdljCli`;
  - `generatedClient` exercises generated-style local client invocation through
    `LocalOrb`;
  - `generatedServer` exercises generated-style servant/dispatcher invocation
    through `LocalOrb`;
  - `namingServer` installs local `NameService`, binds and resolves names, and
    resolves `corbaname:rir:`;
  - `iorDiagnostics` parses deterministic `corbaloc`, `corbaname`, and
    stringified IOR/profile values;
  - `interopReport` serializes and parses structured G6 interop reports.
- `:modules:corba-cdr:nativeCdrSmoke` builds and executes a GraalVM Native
  Image smoke executable for the CDR primitive, string, sequence, and
  encapsulation reader/writer API.
- `:modules:corba-idl-semantics:nativeIdlSemanticsSmoke` builds and executes a
  GraalVM Native Image smoke executable for parser-to-semantics behavior over a
  compact IDL fixture.
- `:modules:corba-idlj-cli:nativeIdljValidateSmoke` builds and executes a
  GraalVM Native Image smoke executable for the validation-only `corba-idlj`
  command path over compact valid and invalid IDL fixtures.
- `:modules:corba-typecode:nativeTypecodeDescriptorSmoke` builds and executes a
  GraalVM Native Image smoke executable for static descriptor construction and
  compile-only codec failure behavior.

## G6-910 class-initialization and metadata policy

G6-910 uses default GraalVM class-initialization behavior for the smoke binaries.
No runtime class-initialization override is required by the local descriptor,
ORB, naming, IOR, IDL validation, or interop-report paths covered here.

The accepted metadata set is empty for:

- reflection configuration;
- dynamic proxy configuration;
- Java serialization configuration;
- service-loader discovery;
- runtime bytecode generation.

Any future metadata file must be committed as reviewed source and added to this
matrix with the owning task and test evidence.

## G6-930 closure evidence

G6-930 keeps Native Image hardening verification source-level and deterministic
by checking that native-image sources do not introduce reflection metadata,
dynamic proxies, service-loader discovery, serialization metadata, runtime
bytecode generation, process execution, internal JDK APIs, or `Unsafe`. The JVM
test lane also repeats representative smoke entrypoints before optional Native
Image compilation.
