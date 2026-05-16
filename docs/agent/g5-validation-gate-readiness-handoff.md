# G5 Validation Gate Readiness Handoff

```text
Task ID: G5-VALIDATION-GATE-READINESS-CLOSURE
Gate: G5 validation readiness preparation
Requirement IDs: REQ-BUILD-001, REQ-BUILD-002, REQ-BUILD-003, REQ-BUILD-004,
  REQ-BUILD-005, REQ-BUILD-006, REQ-BUILD-007, REQ-BUILD-008, REQ-BUILD-009,
  REQ-NATIVE-005, REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003,
  REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007,
  REQ-INTEROP-008, REQ-INTEROP-009
ADR IDs: ADR-0001, ADR-0004, ADR-0005, ADR-0007, ADR-0008, ADR-0009,
  ADR-0010, ADR-0011, ADR-0012
Specification references: repository gate policy, architecture boundaries,
  verification strategy, interop matrix, Native Image matrix, offline build
  validation, and coverage policy
Target module: documentation and gate-readiness evidence
Allowed files:
- docs/verification/g5-validation-gate-readiness.md
- docs/verification/verification-index.md
- docs/agent/agent-handoff.md
- docs/agent/g5-validation-gate-readiness-handoff.md
- docs/charter.md
Forbidden files:
- modules/**/src/main/**
- runtime, protocol, IDL, ORB, POA, service, compiler, or generated behavior
- real interop assertions, peer artifacts, container build outputs, Gradle
  dependency/plugin changes, and generated verification outputs
Expected behavior:
- Record G5 readiness evidence without marking any gate approved.
- Preserve scaffold deferrals for empty ArchUnit matches, permissive JaCoCo
  thresholds, interop artifact resolution, real interop execution, Native Image
  execution, and license/legal approval.
- Update the agent handoff so G5 readiness is the active follow-on task and G6
  implementation remains blocked pending maintainer approval.
Tests to add/update:
- No tests added. Run existing validation and dry-run checks.
Documentation to update:
- Add G5 readiness evidence and this handoff.
- Add the readiness document to the verification index.
- Refresh charter and agent handoff wording without approving gates.
Commands to run:
- ./gradlew validateDesignControlPack qualityGate
- ./gradlew :modules:corba-architecture-tests:test
- ./interop/bin/interop-peer validate-manifests
- for peer in jacorb glassfish-orb jboss-openjdk-orb ace-tao; do interop/peers/${peer}/build-image.sh --dry-run; interop/peers/${peer}/launch.sh --dry-run server; interop/peers/${peer}/health.sh --dry-run; done
- git diff --check
Acceptance criteria:
- All listed commands pass.
- git diff --name-only is limited to the allowed files above.
- No gate is marked approved.
- No production source, runtime behavior, protocol behavior, IDL behavior,
  compiler behavior, generated code, real interop assertion, peer artifact,
  container build output, or Gradle dependency/plugin change is added.
Rollback notes:
- Revert the G5 readiness document, handoff, verification index update, charter
  wording update, and agent handoff update together.
```
