# G10-120-080 Project-Owned Interop Defect Closure

Task ID: G10-120-080-PROJECT-OWNED-INTEROP-DEFECT-CLOSURE
Status: ready-for-implementation
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-008, REQ-INTEROP-009
ADR IDs: ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0013
Specification references: CORBA-IF, CORBA-IOP, IDL-42, I2JAV-13, JAV2I-14, NAM-13
Target module: Project-owned interop defect closure
Allowed files: interop/**, modules/corba-interop-testkit/src/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-120-*.md, README.md
Forbidden files: production behavior changes unless this task is explicitly amended before implementation, Gradle build logic changes, vendored peer source, committed peer binaries, raw live report outputs, optional service implementation, unapproved release version changes
Expected behavior: Task type: implementation. Fix every remaining `our-bug` from G10-120-060 and G10-120-070. Production wire/runtime fixes require an explicit allowed-file amendment before implementation and must be narrow, covered by focused unit tests, and verified by rerunning the failing live lane.
Tests to add/update: Focused unit tests for each fixed defect, plus interop testkit coverage where the ownership evidence or harness classification changes.
Documentation to update: Interop matrix, reference behavior capture, pre-1.0 interop plan, roadmap index, README, this task, and G10-120-090 status when complete.
Commands to run: Focused module tests for each touched production module; ./gradlew :modules:corba-interop-testkit:test; ./interop/bin/interop-peer validate-manifests; INTEROP_ARTIFACT_CACHE=/mnt/d/projects/mundane-java-orb/interop/work/artifact-cache ./interop/bin/interop-peer validate-gates --require-cache; ./gradlew validateDesignControlPack qualityGate; rerun each fixed live lane; git diff --check
Acceptance criteria: Zero `our-bug` classifications remain.
Rollback notes: Revert narrow defect fixes, related tests, and roadmap status updates.
