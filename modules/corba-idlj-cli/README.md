# corba-idlj-cli

idlj-like command-line compiler distribution and CLI entry point.

## Current status

G6-150 implements the first validation-only CLI slice.

```bash
corba-idlj validate [-Ipath|-I path|--include path] [--quiet] <files...>
```

The validate command reads UTF-8 IDL source files, expands bounded includes
through the configured include roots, parses the approved minimal IDL subset,
and runs semantic analysis. It reports stable diagnostics without generating
Java source, CDR codecs, repository IDs, ORB runtime artifacts, protocol
behavior, or Native Image metadata.

Exit codes:

- `0`: validation completed without error diagnostics;
- `1`: one or more IDL parser or semantic error diagnostics were emitted;
- `2`: command-line usage or root source-file input failed.

Diagnostics are written to stderr as:

```text
source.idl:line:column: ERROR CODE: message
```

Diagnostics without a source span are written as:

```text
ERROR CODE: message
```

Successful validation writes `Validated N IDL file(s).` to stdout unless
`--quiet` is present.

Native Image validation:

- `./gradlew :modules:corba-idlj-cli:nativeIdljValidateSmoke` builds and runs a
  compact GraalVM Native Image smoke executable for the validate command when
  `native-image` is available through `NATIVE_IMAGE`, `JAVA_HOME`, a SDKMAN
  GraalVM candidate, or final fallback `PATH` lookup. Prefer setting
  `JAVA_HOME` to the SDKMAN GraalVM candidate and prepending `$JAVA_HOME/bin`
  to `PATH`.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
