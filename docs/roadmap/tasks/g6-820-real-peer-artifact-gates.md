# G6-820 Real Peer Artifact Gates

Task ID: G6-820-REAL-PEER-ARTIFACT-GATES
Status: complete
Gate: G6 interop infrastructure
Requirement IDs: REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009
ADR IDs: ADR-0006, ADR-0010
Specification references: CORBA-IOP-ARCH, CORBA-IOP-GIOP, CORBA-IOP-IIOP
Target module: interop peer infrastructure
Allowed files: interop/**, modules/corba-interop-testkit/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/reference-implementations.md, README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g6-820-real-peer-artifact-gates.md, docs/roadmap/tasks/g6-830-real-peer-interop-reports.md, docs/roadmap/tasks/g6-910-native-image-binaries.md
Forbidden files: vendored peer source, committed peer binaries, generated container outputs, reference implementation source copying
Expected behavior: Task type: human-gate-blocked. Define and approve artifact acquisition, cache layout, license review evidence, and clean-room controls before real peer execution.
Tests to add/update: Manifest validation, gate validation, cache validation, dry-run checks, and blocked real-execution checks.
Documentation to update: Peer manifests, interop matrix, and reference behavior capture docs.
Commands to run: ./interop/bin/interop-peer validate-manifests; ./interop/bin/interop-peer validate-gates; INTEROP_ARTIFACT_CACHE=/absolute/approved/cache ./interop/bin/interop-peer validate-gates --require-cache; INTEROP_ARTIFACT_CACHE=/absolute/approved/cache ./interop/bin/interop-peer validate-gates --require-cache jacorb; ./gradlew :modules:corba-interop-testkit:test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Real peer execution remains blocked until license/artifact gates are explicitly recorded.
Rollback notes: Revert interop artifact-gate docs and manifest updates together.
