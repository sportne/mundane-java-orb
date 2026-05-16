# G6-010 Gate State and Control

Task ID: G6-010-GATE-STATE-AND-CONTROL
Gate: G6 roadmap control
Requirement IDs: REQ-DOC-006, REQ-NFR-007, REQ-BUILD-007, REQ-BUILD-008
ADR IDs: ADR-0001, ADR-0004, ADR-0005, ADR-0008
Specification references: Operational gate-control task; no direct OMG clause.
Target module: documentation and gate-control files
Allowed files: docs/agent/**, docs/verification/**, docs/roadmap/**
Forbidden files: modules/**/src/main/**, runtime behavior, protocol behavior, IDL behavior, compiler behavior, generated code
Expected behavior: Task type: design-only. Record G0-G5 approval state, define G6 handoff rules, and keep implementation blocked unless a narrow task handoff exists.
Tests to add/update: No product tests; run design-control validation.
Documentation to update: Agent handoff, G6 control notes, roadmap index.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: G6 execution rules are clear and no product implementation is added.
Rollback notes: Revert gate-control documentation changes together.

