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

G6-220 starts the generated descriptor path with static metadata values and
compile-only codec surfaces. A narrow Native Image smoke check validates that
descriptor construction and unsupported codec failures work without reflection,
service loading, or dynamic class discovery.

G6-910 adds the central `corba-native-image` verification harness. It discovers
Native Image toolchains deterministically, models the approved smoke binaries as
stable targets, and builds each executable with `--no-fallback` and
`-H:+ReportExceptionStackTraces`. The target set covers IDL validation,
generated-style local client and server dispatch, local naming, IOR diagnostics,
and structured interop report serialization.

The G6-910 harness is verification-only. Production runtime modules still do not
spawn processes, scan classpaths, use service loading, generate bytecode at
runtime, or require reflection/proxy/serialization metadata for the covered
paths. Unit tests audit the harness and smoke sources for those forbidden
tokens, and the native matrix records the empty metadata policy.

## Native test targets

- `idlj` CLI.
- client sample.
- server sample.
- naming server.
- selected interop clients and servers.
