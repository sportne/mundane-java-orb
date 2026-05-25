# G10-120-070 Live Matrix Reporting And Classification

Task ID: G10-120-070-LIVE-MATRIX-REPORTING-AND-CLASSIFICATION
Status: blocked
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-008, REQ-INTEROP-009
ADR IDs: ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0013
Specification references: CORBA-IF, CORBA-IOP, IDL-42, I2JAV-13, JAV2I-14, NAM-13
Target module: Live interop report aggregation and classification
Allowed files: interop/**, modules/corba-interop-testkit/src/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-120-*.md, README.md
Forbidden files: production behavior changes, Gradle build logic changes, vendored peer source, committed peer binaries, raw live report outputs, optional service implementation, unapproved release version changes
Expected behavior: Task type: implementation. Add deterministic report aggregation and summary checks if current reports are insufficient for clean-room evidence, and normalize final classifications to `passed`, `peer-bug`, `spec-ambiguity`, `profile-mismatch`, or `our-bug`.
Tests to add/update: Interop testkit coverage for report aggregation completeness, classification vocabulary, and exclusion of raw logs, IORs, peer artifacts, native binaries, Docker layers, and downloaded artifacts from committed evidence.
Documentation to update: Interop matrix, reference behavior capture, pre-1.0 interop plan, roadmap index, README, this task, and G10-120-080 status when complete.
Commands to run: ./gradlew :modules:corba-interop-testkit:test; ./interop/bin/interop-peer validate-manifests; INTEROP_ARTIFACT_CACHE=/mnt/d/projects/mundane-java-orb/interop/work/artifact-cache ./interop/bin/interop-peer validate-gates --require-cache; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Full matrix execution produces a complete structured summary for all peers, scenarios, and directions, with no raw live outputs committed.
Rollback notes: Revert report aggregation/classification changes and roadmap status updates.
