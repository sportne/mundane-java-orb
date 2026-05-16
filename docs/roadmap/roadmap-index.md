# G6 Roadmap Index

This roadmap is a draft implementation task set. It favors functional,
testable vertical slices while preserving the dependency order needed for a
large CORBA implementation.

Each task file under `docs/roadmap/tasks/` uses the fields from
`docs/agent/implementation-task-template.md`. A task is not approved for
execution until it is copied or narrowed into a task-specific handoff.

## Status vocabulary

- `draft`: Roadmap task exists but is not approved for execution.
- `ready-for-handoff`: Task is the next candidate and needs exact final scope.
- `blocked`: Task depends on a separate design, human, license, or artifact gate.
- `complete`: Task has been implemented and accepted.

## Ordered task set

### G6.0 Control

- `tasks/g6-000-roadmap-task-set.md`
- `tasks/g6-010-gate-state-and-control.md`
- `tasks/g6-020-coverage-architecture-tightening.md`

### G6.1 Foundations

- `tasks/g6-030-common-diagnostics-limits.md`
- `tasks/g6-040-repository-id-foundation.md`
- `tasks/g6-050-testkit-golden-fixtures.md`

### G6.2 IDL Compiler Slice

- `tasks/g6-110-idl-diagnostics-lexer.md`
- `tasks/g6-120-idl-preprocessor-includes.md`
- `tasks/g6-130-idl-minimal-parser-ast.md`
- `tasks/g6-140-idl-semantics-symbols.md`
- `tasks/g6-150-idlj-validate-cli.md`
- `tasks/g6-160-idl-java-minimal-generation.md`

### G6.3 Generated Hello Slice

- `tasks/g6-210-generated-hello-golden-source.md`
- `tasks/g6-220-generated-codecs-descriptors.md`

### G6.4 CDR and IOR Slice

- `tasks/g6-310-cdr-primitives.md`
- `tasks/g6-320-cdr-strings-sequences-encapsulations.md`
- `tasks/g6-330-ior-profiles-object-urls.md`

### G6.5 Local Invocation Slice

- `tasks/g6-410-local-object-reference-invocation.md`
- `tasks/g6-420-exception-mapping.md`

### G6.6 Wire Invocation Slice

- `tasks/g6-510-giop-messages.md`
- `tasks/g6-520-iiop-tcp.md`
- `tasks/g6-530-iiop-tls-mtls.md`

### G6.7 Server Runtime Slice

- `tasks/g6-610-poa-policy-matrix.md`
- `tasks/g6-620-poa-lite-servant-dispatch.md`
- `tasks/g6-630-full-poa-policy-expansion.md`

### G6.8 Dynamic and Metadata Slice

- `tasks/g6-710-typecode-any.md`
- `tasks/g6-720-dynamicany-dii-dsi.md`
- `tasks/g6-730-interface-repository-static-metadata.md`

### G6.9 Naming and Interop Slice

- `tasks/g6-810-cosnaming-vertical-slice.md`
- `tasks/g6-820-real-peer-artifact-gates.md`
- `tasks/g6-830-real-peer-interop-reports.md`

### G6.10 Native and Release Hardening

- `tasks/g6-910-native-image-binaries.md`
- `tasks/g6-920-offline-release-validation.md`
- `tasks/g6-930-compatibility-security-performance-closure.md`

### Deferred gated tasks

- `tasks/g6-d10-rmi-iiop-java-to-idl.md`
- `tasks/g6-d20-optional-corba-services.md`
- `tasks/g6-d30-legal-public-release.md`

