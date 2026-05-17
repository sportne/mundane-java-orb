# Native Image Matrix

## Toolchains

- GraalVM JDK 21 Native Image.
- GraalVM JDK 25 Native Image.

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
