# G13-000 Durable Runtime Hardening Task Set

Task ID: G13-000-DURABLE-RUNTIME-HARDENING-TASK-SET
Status: complete
Gate: G13 durable runtime hardening
Requirement IDs: REQ-ORB-001, REQ-POA-001, REQ-POA-002, REQ-IOR-001, REQ-IOR-002, REQ-NAM-001, REQ-INTEROP-009, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0003, ADR-0005, ADR-0008, ADR-0010, ADR-0014
Specification references: CORBA-IF-ORB, CORBA-IF-POA, CORBA-IF-OBJECT-REF, CORBA-IOP-IOR, CORBA-IOP-IIOP, NAM-13-SERVICE, NAM-13-URLS
Target module: documentation roadmap only
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g13-*.md
Forbidden files: production source, test source, Gradle build logic, generated artifacts, peer artifacts, interop cache files, live interop reports, optional service implementation
Expected behavior: Task type: design-only. Define the G13 durable runtime hardening roadmap after G12 durable identity implementation, with local cross-process restart evidence before operational store hardening, store versioning policy, POA rehydration design, or live peer persistence design.
Tests to add/update: No product tests; validate the documentation control pack.
Documentation to update: Add this task set, the G13 task sequence, README ready-task status, and roadmap index entries.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: G13 task ordering is explicit; G13-010 is the only non-human-gated ready task; later G13 tasks are blocked behind local evidence or design prerequisites; live peer execution remains unapproved; no runtime, generated-code, build, peer-cache, or live interop changes are made.
Rollback notes: Revert the G13 roadmap task files, README status update, and roadmap index update together.
