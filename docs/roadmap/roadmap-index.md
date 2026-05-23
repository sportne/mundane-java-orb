# Roadmap Index

This roadmap is the source of truth for implementation task sets. It favors
functional, testable vertical slices while preserving the dependency order
needed for a large CORBA implementation.

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

- `tasks/g6-910-native-image-binaries.md` - complete
- `tasks/g6-920-offline-release-validation.md` - complete
- `tasks/g6-930-compatibility-security-performance-closure.md` - complete

## G7 RMI-IIOP and Java-to-IDL

- `tasks/g7-000-rmi-iiop-task-set.md` - complete
- `tasks/g7-010-rmi-java-eligibility-diagnostics.md` - complete
- `tasks/g7-020-java-to-idl-model.md` - complete
- `tasks/g7-030-rmi-repository-id-hashes.md` - complete
- `tasks/g7-040-generated-idl-fixtures.md` - complete
- `tasks/g7-050-rmi-binding-generation.md` - complete
- `tasks/g7-060-rmi-value-exception-marshaling.md` - complete
- `tasks/g7-070-local-rmi-iiop-adapters.md` - complete
- `tasks/g7-080-rmi-iiop-wire-integration.md` - complete
- `tasks/g7-090-rmi-iiop-peer-interop.md` - complete
- `tasks/g7-100-rmi-iiop-native-security-closure.md` - complete

## G8 Optional CORBA Services

- `tasks/g6-d20-optional-corba-services.md` - complete
- `tasks/g8-d10-trading-service-design-gate.md` - human-gate-blocked
- `tasks/g8-d20-event-service-design-gate.md` - human-gate-blocked
- `tasks/g8-d30-notification-service-design-gate.md` - human-gate-blocked
- `tasks/g8-d40-transaction-service-design-gate.md` - human-gate-blocked
- `tasks/g8-d50-security-service-design-gate.md` - human-gate-blocked
- `tasks/g8-d60-time-service-design-gate.md` - human-gate-blocked

## G9 Verification Hardening

- `tasks/g9-010-exhaustive-test-hardening.md` - complete

## G10 Pre-1.0 Interoperability

- `tasks/g10-000-pre-1-0-interop-task-set.md` - complete
- `tasks/g10-010-idl-4-2-grammar-closure.md` - complete
- `tasks/g10-020-idl-to-java-legacy-mapping-closure.md` - complete
- `tasks/g10-030-omg-api-compatibility-surface.md` - ready-for-implementation
- `tasks/g10-040-cdr-giop-ior-wire-closure.md` - blocked
- `tasks/g10-050-network-orb-poa-dispatch.md` - blocked
- `tasks/g10-060-network-naming-and-urls.md` - blocked
- `tasks/g10-070-dynamic-any-dii-dsi-and-ir-wire-closure.md` - blocked
- `tasks/g10-080-portable-interceptors.md` - blocked
- `tasks/g10-090-rmi-iiop-compatibility-closure.md` - blocked
- `tasks/g10-100-native-image-interop-binaries.md` - blocked
- `tasks/g10-110-real-peer-harness-closure.md` - blocked
- `tasks/g10-120-pre-1-0-full-interop-execution.md` - blocked
