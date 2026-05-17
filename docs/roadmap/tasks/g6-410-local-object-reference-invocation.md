# G6-410 Local Object Reference Invocation

Task ID: G6-410-LOCAL-OBJECT-REFERENCE-INVOCATION
Status: complete
Gate: G6 local invocation vertical slice
Requirement IDs: REQ-ORB-001, REQ-IDLJ-004, REQ-NFR-001, REQ-NFR-003, REQ-DOC-001
ADR IDs: ADR-0002, ADR-0003, ADR-0005, ADR-0010
Specification references: CORBA-IF-ORB, CORBA-IF-OBJECT-REF, CORBA-IF-MESSAGING
Target module: modules/corba-orb-core, modules/corba-modern-api
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-410-local-object-reference-invocation.md, docs/roadmap/tasks/g6-420-exception-mapping.md, modules/corba-orb-core/build.gradle, modules/corba-orb-core/README.md, modules/corba-orb-core/src/**, modules/corba-modern-api/build.gradle, modules/corba-modern-api/README.md, modules/corba-modern-api/src/**, docs/architecture/runtime-architecture.md
Forbidden files: network transport, full POA policy behavior, external peer interop
Expected behavior: Task type: implementation. Add an in-process object reference and invocation path for the generated Hello slice.
Tests to add/update: Unit and integration tests for object reference identity, request dispatch, shutdown behavior, and no-network local calls.
Documentation to update: Runtime architecture and package docs.
Commands to run: ./gradlew :modules:corba-orb-core:test :modules:corba-modern-api:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: A generated client can call a local generated server path without IIOP.
Rollback notes: Revert local invocation implementation, tests, and docs together.
