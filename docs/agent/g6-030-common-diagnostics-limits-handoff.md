# G6-030 Common Diagnostics and Limits Handoff

```text
Task ID: G6-030-COMMON-DIAGNOSTICS-LIMITS
Gate: G6 foundation implementation
Requirement IDs: REQ-IDL-003, REQ-SEC-001, REQ-SEC-002, REQ-NFR-004,
  REQ-DOC-001, REQ-DOC-003
ADR IDs: ADR-0004, ADR-0005, ADR-0010
Specification references: IDL-42-LEXICAL, IDL-42-GRAMMAR, CORBA-IOP-CDR,
  CORBA-IOP-GIOP
Target module: modules/corba-common
Allowed files:
- modules/corba-common/src/main/**
- modules/corba-common/src/test/**
- modules/corba-common/README.md
- docs/architecture/common-foundations.md
- docs/architecture/architecture-index.md
- docs/agent/agent-handoff.md
- docs/agent/g6-030-common-diagnostics-limits-handoff.md
- docs/roadmap/roadmap-index.md
Forbidden files:
- protocol encoders/decoders outside common
- ORB runtime behavior
- IDL parser behavior outside diagnostics support
- generated code, Gradle dependency/plugin changes, peer artifacts, and
  container build outputs
Expected behavior:
- Implement shared immutable diagnostic, source-location, and bounded-limit
  value objects in `io.github.mundanej.mjo.common`.
- Keep the slice free of parser behavior, CDR/GIOP/IIOP encoding, ORB runtime
  behavior, generated code, reflection, and Java serialization.
Tests to add/update:
- Unit tests for diagnostic-code validation, source position/span validation,
  diagnostic construction, bounded-limit checks, and record equality.
Documentation to update:
- Common package docs, module README, common foundations architecture note,
  roadmap index, and agent handoff.
Commands to run:
- ./gradlew :modules:corba-common:test
- ./gradlew validateDesignControlPack qualityGate
- git diff --check
Acceptance criteria:
- All listed commands pass.
- Common types are public, documented, warning-free, and covered by unit tests.
- Changed files stay within this handoff's allowed files plus the paired
  G6-010 control handoff.
Rollback notes:
- Revert common diagnostics and limit types with their tests/docs.
```
