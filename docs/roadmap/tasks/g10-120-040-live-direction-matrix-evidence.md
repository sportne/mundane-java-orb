# G10-120-040 Live Direction Matrix Evidence

Task ID: G10-120-040-LIVE-DIRECTION-MATRIX-EVIDENCE
Status: ready-for-implementation
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009, REQ-NATIVE-002
ADR IDs: ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0008, ADR-0010, ADR-0013
Specification references: CORBA-IF, CORBA-IOP, IDL-42, I2JAV-13, JAV2I-14, NAM-13
Target module: interop verification and documentation
Allowed files: interop/**, modules/corba-interop-testkit/src/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/conformance/*.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-120-*.md, README.md
Forbidden files: production behavior changes, Gradle build logic changes, vendored peer source, committed peer binaries, committed native binaries, raw live report outputs, optional service implementation, unapproved release version changes
Expected behavior: Task type: verification-only. Build approved peer images and Native Image binaries, run the complete non-optional live direction matrix, record clean-room evidence summaries, and mark the parent G10-120 task complete only when every required lane passes or has maintainer-approved non-our-bug classification.
Tests to add/update: No product tests unless final report aggregation or evidence validation gaps are found.
Documentation to update: Interop matrix, reference behavior capture, pre-1.0 interop plan, conformance matrices, roadmap index, README, parent G10-120 task, and this task.
Commands to run: ./gradlew test; ./gradlew validateDesignControlPack qualityGate; ./interop/bin/interop-peer validate-manifests; INTEROP_ARTIFACT_CACHE=/mnt/d/projects/mundane-java-orb/interop/work/artifact-cache ./interop/bin/interop-peer validate-gates --require-cache; ./gradlew :modules:corba-native-image:nativeImageBinariesSmoke; ./interop/bin/interop-peer run-direction-matrix --require-live <scenario> all; git diff --check
Acceptance criteria: Full non-optional pre-1.0 live interop evidence is summarized in docs, parent G10-120 is complete, no non-human-gated ready child task remains, and optional services remain deferred.
Rollback notes: Revert clean-room evidence documentation and roadmap completion status; do not revert completed prerequisite implementation tasks.
