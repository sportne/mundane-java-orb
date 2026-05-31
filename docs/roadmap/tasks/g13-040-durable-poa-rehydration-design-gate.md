# G13-040 Durable POA Rehydration Design Gate

Task ID: G13-040-DURABLE-POA-REHYDRATION-DESIGN-GATE
Status: ready-for-implementation
Gate: G13 durable runtime hardening
Requirement IDs: REQ-ORB-001, REQ-POA-001, REQ-POA-002, REQ-IOR-001, REQ-IOR-002, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0003, ADR-0005, ADR-0008, ADR-0010, ADR-0014
Specification references: CORBA-IF-ORB, CORBA-IF-POA, CORBA-IF-OBJECT-REF, CORBA-IOP-IOR, CORBA-IOP-IIOP
Target module: durable POA design documentation
Allowed files: docs/adr/*.md, docs/architecture/runtime-architecture.md, docs/architecture/poa-design.md, docs/architecture/cdr-giop-iiop.md, docs/conformance/corba-3.4-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g13-040-durable-poa-rehydration-design-gate.md, docs/roadmap/tasks/g13-050-live-peer-durable-ior-naming-design.md, docs/roadmap/tasks/g13-*.md, README.md
Forbidden files: production source, test source, Gradle build logic, generated artifacts, peer artifacts, committed live interop reports, optional service implementation
Expected behavior: Task type: design-only. Blocked until G13-010 completes. Decide whether durable POA restart remains caller-managed or grows POA-managed lazy reactivation behavior, then record the decision and create blocked follow-on implementation tasks only if POA-managed behavior is approved.
Tests to add/update: No product tests; validate the documentation control pack.
Documentation to update: ADRs as needed, runtime architecture, POA design, CDR/GIOP/IIOP architecture if routing changes are approved, CORBA conformance matrix, roadmap index, README ready-task status, this task, and G13-050 status when complete.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: The design states caller responsibilities, POA responsibilities, durable key lookup semantics, servant-manager implications, Native Image constraints, and security/hostile-key behavior; if POA-managed rehydration is approved, blocked tasks are created for POA path registry, adapter activation lookup, servant-manager behavior, and IIOP durable-key routing; G13-050 is promoted to ready-for-implementation.
Rollback notes: Revert the durable POA rehydration design docs and any follow-on roadmap task files together.
