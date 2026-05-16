# G6 Project Roadmap Handoff

```text
Task ID: G6-000-ROADMAP-TASK-SET
Gate: G6 roadmap control
Requirement IDs: REQ-DOC-006, REQ-NFR-007
ADR IDs: ADR-0001, ADR-0004, ADR-0005
Specification references: Operational roadmap task; no direct OMG clause.
Target module: docs/roadmap
Allowed files:
- docs/roadmap/roadmap-index.md
- docs/roadmap/tasks/*.md
- docs/agent/agent-handoff.md
- docs/agent/g6-project-roadmap-handoff.md
Forbidden files:
- modules/**/src/main/**
- runtime, protocol, IDL, ORB, POA, service, compiler, or generated behavior
- real interop assertions, peer artifacts, container build outputs, Gradle
  dependency/plugin changes, and generated verification outputs
Expected behavior:
- Create independent roadmap task files using every field from
  docs/agent/implementation-task-template.md.
- Favor functional, testable vertical slices while retaining phase ordering and
  explicit dependencies.
- Record G0-G5 maintainer approval and keep implementation blocked until a
  roadmap task is narrowed into a task-specific G6 handoff.
Tests to add/update:
- No product tests. Run existing validation.
Documentation to update:
- Add roadmap index and task files.
- Update agent handoff with the approved-gate state and active G6 roadmap step.
Commands to run:
- ./gradlew validateDesignControlPack qualityGate
- git diff --check
Acceptance criteria:
- All listed commands pass.
- Every roadmap task file contains the required template fields.
- Every task Expected behavior starts with Task type: design-only,
  implementation, verification-only, or human-gate-blocked.
- No roadmap task is marked implemented or complete.
- No production source, runtime behavior, protocol behavior, IDL behavior,
  compiler behavior, generated code, real interop assertion, peer artifact,
  container build output, or Gradle dependency/plugin change is added.
Rollback notes:
- Revert the roadmap directory, this handoff, and agent handoff update together.
```
