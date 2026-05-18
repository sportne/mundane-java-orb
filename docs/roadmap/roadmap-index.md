# G6 Roadmap Index

This roadmap is the G6 implementation task set. It favors functional, testable
vertical slices while preserving the dependency order needed for a large CORBA
implementation.

Each task file under `docs/roadmap/tasks/` uses the fields from
`docs/roadmap/implementation-task-template.md`. After G0-G5 approval, these task
files are the source of truth for G6 execution state. A task may be implemented
when its `Status:` is `ready-for-implementation`, its scope is specific enough
to execute directly, and its file boundaries do not conflict with other active
work.

`validateDesignControlPack` enforces the task template shape, accepted status
values, task-type markers, and the rule that at least one task is active as
`ready-for-implementation` or `in-progress` while open roadmap work remains.

## Status vocabulary

- `draft`: Task exists but is not approved for execution.
- `ready-for-implementation`: Task is approved, unblocked, and available for execution.
- `in-progress`: Task is actively being executed; file boundaries must not conflict with other active work.
- `complete`: Task has been implemented and accepted.
- `blocked`: Task depends on another implementation, design, artifact, or validation task.
- `human-gate-blocked`: Task depends on maintainer, legal, license, or release approval.

## Ordered task set

### G6.0 Control

- `tasks/g6-000-roadmap-task-set.md`
- `tasks/g6-010-gate-state-and-control.md` - complete
- `tasks/g6-020-coverage-architecture-tightening.md` - complete

### G6.1 Foundations

- `tasks/g6-030-common-diagnostics-limits.md` - complete
- `tasks/g6-040-repository-id-foundation.md` - complete
- `tasks/g6-050-testkit-golden-fixtures.md` - complete

### G6.2 IDL Compiler Slice

- `tasks/g6-110-idl-diagnostics-lexer.md` - complete
- `tasks/g6-120-idl-preprocessor-includes.md` - complete
- `tasks/g6-130-idl-minimal-parser-ast.md` - complete
- `tasks/g6-140-idl-semantics-symbols.md` - complete
- `tasks/g6-150-idlj-validate-cli.md` - complete
- `tasks/g6-160-idl-java-minimal-generation.md` - complete

### G6.3 Generated Hello Slice

- `tasks/g6-210-generated-hello-golden-source.md` - complete
- `tasks/g6-220-generated-codecs-descriptors.md` - complete

### G6.4 CDR and IOR Slice

- `tasks/g6-310-cdr-primitives.md` - complete
- `tasks/g6-320-cdr-strings-sequences-encapsulations.md` - complete
- `tasks/g6-330-ior-profiles-object-urls.md` - complete

### G6.5 Local Invocation Slice

- `tasks/g6-410-local-object-reference-invocation.md` - complete
- `tasks/g6-420-exception-mapping.md` - complete

### G6.6 Wire Invocation Slice

- `tasks/g6-510-giop-messages.md` - complete
- `tasks/g6-520-iiop-tcp.md` - complete
- `tasks/g6-530-iiop-tls-mtls.md` - complete

### G6.7 Server Runtime Slice

- `tasks/g6-610-poa-policy-matrix.md` - complete
- `tasks/g6-620-poa-lite-servant-dispatch.md` - complete
- `tasks/g6-630-full-poa-policy-expansion.md` - complete

### G6.8 Dynamic and Metadata Slice

- `tasks/g6-710-typecode-any.md` - complete
- `tasks/g6-720-dynamicany-dii-dsi.md` - complete
- `tasks/g6-730-interface-repository-static-metadata.md` - complete

### G6.9 Naming and Interop Slice

- `tasks/g6-810-cosnaming-vertical-slice.md` - complete
- `tasks/g6-820-real-peer-artifact-gates.md` - complete
- `tasks/g6-830-real-peer-interop-reports.md` - complete

### G6.10 Native and Release Hardening

- `tasks/g6-910-native-image-binaries.md` - ready-for-implementation
- `tasks/g6-920-offline-release-validation.md`
- `tasks/g6-930-compatibility-security-performance-closure.md`

### Deferred gated tasks

- `tasks/g6-d10-rmi-iiop-java-to-idl.md`
- `tasks/g6-d20-optional-corba-services.md`
- `tasks/g6-d30-legal-public-release.md`
