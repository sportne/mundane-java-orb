# G10-000 Pre-1.0 Interop Task Set

Task ID: G10-000-PRE-1.0-INTEROP-TASK-SET
Status: complete
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009
ADR IDs: ADR-0001, ADR-0003, ADR-0004, ADR-0005, ADR-0006, ADR-0007, ADR-0008, ADR-0010, ADR-0013
Specification references: CORBA-IF, CORBA-IOP, IDL-42, I2JAV-13, JAV2I-14, NAM-13
Target module: documentation roadmap only
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-*.md, docs/verification/verification-index.md, docs/verification/pre-1-0-interoperability-plan.md, docs/verification/interop-matrix.md
Forbidden files: production source, test source, Gradle build logic, generated artifacts, peer artifacts, interop cache files, live interop reports, optional service implementation
Expected behavior: Task type: design-only. Define the G10 pre-1.0 roadmap for completing non-optional CORBA interoperability before live full-matrix execution.
Tests to add/update: No product tests; validate the documentation control pack.
Documentation to update: Add this task set, the G10 task sequence, the pre-1.0 interop plan, README ready-task status, roadmap index, verification index, and interop matrix note.
Commands to run: ./gradlew validateDesignControlPack; git diff --check
Acceptance criteria: G10 task ordering is explicit; G10-010 is the only non-human-gated ready task; all later G10 tasks are blocked; optional services remain human-gate-blocked; no runtime, generated-code, build, peer-cache, or live interop changes are made.
Rollback notes: Revert the G10 roadmap task files and verification documentation together.
