# G10-120-090 Final Pre-1.0 Live Evidence

Task ID: G10-120-090-FINAL-PRE-1.0-LIVE-EVIDENCE
Status: blocked
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009, REQ-NATIVE-002
ADR IDs: ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0008, ADR-0010, ADR-0013
Specification references: CORBA-IF, CORBA-IOP, IDL-42, I2JAV-13, JAV2I-14, NAM-13
Target module: Final G10-120 live evidence documentation
Allowed files: docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/conformance/*.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-120-*.md, README.md
Forbidden files: production behavior changes, test source changes, Gradle build logic changes, interop harness behavior changes, vendored peer source, committed peer binaries, committed native binaries, raw live report outputs, optional service implementation, unapproved release version changes
Expected behavior: Task type: verification-only. Run the complete approved live matrix for all six non-optional scenarios and all four peers, record clean-room evidence summaries in docs, mark the parent G10-120 task complete, and leave no non-human-gated ready G10 task unless a new blocker task is intentionally created.
Tests to add/update: None unless final evidence validation gaps are found.
Documentation to update: Interop matrix, reference behavior capture, pre-1.0 interop plan, conformance matrices, roadmap index, README, parent G10-120 task, and this task.
Commands to run: ./gradlew test; ./gradlew :modules:corba-native-image:nativeImageBinariesSmoke; ./gradlew validateDesignControlPack qualityGate; ./interop/bin/interop-peer validate-manifests; INTEROP_ARTIFACT_CACHE=/mnt/d/projects/mundane-java-orb/interop/work/artifact-cache ./interop/bin/interop-peer validate-gates --require-cache; ./interop/bin/interop-peer run-direction-matrix --require-live <scenario> all for `basic-idl`, `object-reference`, `naming`, `giop`, `iiop`, and `rmi-iiop`; git diff --check
Acceptance criteria: All required gates pass, optional services remain human-gated, parent G10-120 is complete, and README/roadmap report no ready task unless a new blocker task is intentionally created.
Rollback notes: Revert clean-room final evidence documentation and roadmap completion status; do not revert completed prerequisite implementation tasks.
