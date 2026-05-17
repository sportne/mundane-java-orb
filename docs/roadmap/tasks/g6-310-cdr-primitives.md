# G6-310 CDR Primitives

Task ID: G6-310-CDR-PRIMITIVES
Status: complete
Gate: G6 CDR and IOR vertical slice
Requirement IDs: REQ-CDR-001, REQ-SEC-001, REQ-SEC-004, REQ-DOC-004
ADR IDs: ADR-0002, ADR-0004, ADR-0005, ADR-0010
Specification references: CORBA-IOP-CDR
Target module: modules/corba-cdr
Allowed files: modules/corba-cdr/build.gradle, modules/corba-cdr/src/main/**, modules/corba-cdr/src/test/**, modules/corba-cdr/README.md, modules/corba-testkit/src/**, docs/architecture/cdr-giop-iiop.md, docs/conformance/corba-3.4-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-310-cdr-primitives.md, README.md
Forbidden files: GIOP/IIOP transport, ORB invocation runtime, IDL parser behavior
Expected behavior: Task type: implementation. Implement bounded CDR primitive read/write behavior with explicit endian and alignment handling.
Tests to add/update: Unit, spec, negative, golden-wire, and native-image smoke tests for primitive values and alignment.
Documentation to update: CDR design notes, package docs, module README, roadmap status, and conformance rows.
Commands to run: ./gradlew :modules:corba-cdr:test; ./gradlew :modules:corba-cdr:nativeCdrSmoke; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Primitive wire encodings are deterministic, bounded, covered by golden-wire fixtures, and validated through the CDR Native Image smoke binary.
Rollback notes: Revert CDR primitive implementation, tests, and docs together.
