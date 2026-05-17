# Native Image Design

## Rules

Detailed enforceable rules are listed in `architecture-rule-catalog.md`.

- Generate stubs, skeletons, codecs, descriptors, and metadata at build time.
- Avoid reflection in normal paths.
- Avoid dynamic proxies in core runtime.
- Avoid runtime bytecode generation.
- Avoid Java serialization for normal CORBA marshaling.
- Avoid `ServiceLoader`, `ClassLoader`, classpath scanning, internal JDK APIs,
  Java serialization hooks, forced GC, and process spawning in production paths
  unless a narrow ADR grants an exception.
- Document runtime class initialization choices.

## Native test targets

- `idlj` CLI.
- client sample.
- server sample.
- naming server.
- selected interop clients and servers.
