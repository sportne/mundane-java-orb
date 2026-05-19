# G7-000 RMI-IIOP Task Set

Task ID: G7-000-RMI-IIOP-TASK-SET
Status: complete
Gate: G7 RMI-IIOP roadmap control
Requirement IDs: REQ-RMI-001, REQ-DOC-006, REQ-NFR-007
ADR IDs: ADR-0001, ADR-0003, ADR-0004, ADR-0005, ADR-0010, ADR-0013
Specification references: JAV2I-14, JAV2I-14-RMI-IDL
Target module: docs/roadmap
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g7-*.md
Forbidden files: modules/**/src/main/**, runtime behavior, protocol behavior, compiler behavior, generated code, Gradle dependency/plugin changes
Expected behavior: Task type: design-only. Create the G7 roadmap task set for the complete RMI-IIOP and Java-to-IDL topic after ADR-0013 acceptance.
Tests to add/update: No product tests; run documentation and quality gates.
Documentation to update: Roadmap index and G7 roadmap task files.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Every G7 roadmap task uses the implementation task template fields, G7-010 is the first task ready for implementation when the task set is created, later G7 tasks are blocked, and no product behavior is implemented by G7-000.
Rollback notes: Revert G7 roadmap task files and roadmap-index changes together.
