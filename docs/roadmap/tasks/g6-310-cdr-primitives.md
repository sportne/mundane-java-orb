# G6-310 CDR Primitives

Task ID: G6-310-CDR-PRIMITIVES
Gate: G6 CDR and IOR vertical slice
Requirement IDs: REQ-CDR-001, REQ-SEC-001, REQ-SEC-004, REQ-DOC-004
ADR IDs: ADR-0002, ADR-0004, ADR-0005, ADR-0010
Specification references: CORBA-IOP-CDR
Target module: modules/corba-cdr
Allowed files: modules/corba-cdr/src/main/**, modules/corba-cdr/src/test/**, modules/corba-testkit/src/**, docs/conformance/corba-3.4-matrix.md
Forbidden files: GIOP/IIOP transport, ORB invocation runtime, IDL parser behavior
Expected behavior: Task type: implementation. Implement bounded CDR primitive read/write behavior with explicit endian and alignment handling.
Tests to add/update: Unit, spec, negative, and golden-wire tests for primitive values and alignment.
Documentation to update: CDR design notes, package docs, and conformance rows.
Commands to run: ./gradlew :modules:corba-cdr:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Primitive wire encodings are deterministic, bounded, and covered by golden-wire fixtures.
Rollback notes: Revert CDR primitive implementation, tests, and docs together.

