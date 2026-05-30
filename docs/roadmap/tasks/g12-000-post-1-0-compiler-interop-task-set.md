# G12-000 Post-1.0 Compiler And Interop Task Set

Task ID: G12-000-POST-1.0-COMPILER-INTEROP-TASK-SET
Status: complete
Gate: G12 post-1.0 compiler and interop hardening
Requirement IDs: REQ-IDL-001, REQ-IDL-002, REQ-IDL-003, REQ-IDLJ-001, REQ-IDLJ-002, REQ-IDLJ-004, REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009, REQ-NATIVE-005, REQ-SEC-003
ADR IDs: ADR-0001, ADR-0003, ADR-0004, ADR-0005, ADR-0006, ADR-0007, ADR-0008, ADR-0010, ADR-0013
Specification references: IDL-42-LEXICAL, IDL-42-PREPROCESSING, IDL-42-GRAMMAR, IDL-42-SCOPING, I2JAV-13, CORBA-IOP-CDR, CORBA-IOP-GIOP, CORBA-IOP-IIOP
Target module: documentation roadmap only
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g12-*.md
Forbidden files: production source, test source, Gradle build logic, generated artifacts, peer artifacts, interop cache files, live interop reports, optional service implementation
Expected behavior: Task type: design-only. Define the G12 post-1.0 roadmap with IDL compiler hardening as the first execution lane, followed by broad IDL fixture interop and later runtime identity work.
Tests to add/update: No product tests; validate the documentation control pack.
Documentation to update: Add this task set, the G12 task sequence, README ready-task status, and roadmap index entries.
Commands to run: ./gradlew validateDesignControlPack; git diff --check
Acceptance criteria: G12 task ordering is explicit; G12-010 is the only non-human-gated ready task; later G12 tasks are blocked; optional services remain human-gate-blocked; no runtime, generated-code, build, peer-cache, or live interop changes are made.
Rollback notes: Revert the G12 roadmap task files, README status update, and roadmap index update together.
