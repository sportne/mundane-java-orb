# G12-060 Peer IDL Feature Interop Matrix

Task ID: G12-060-PEER-IDL-FEATURE-INTEROP-MATRIX
Status: blocked
Gate: G12 post-1.0 compiler and interop hardening
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009, REQ-NATIVE-005, REQ-SEC-003
ADR IDs: ADR-0001, ADR-0005, ADR-0006, ADR-0008, ADR-0010
Specification references: IDL-42-GRAMMAR, I2JAV-13, CORBA-IOP-CDR, CORBA-IOP-GIOP, CORBA-IOP-IIOP
Target module: modules/corba-interop-testkit, interop
Allowed files: modules/corba-interop-testkit/src/**, modules/corba-native-image/src/**, interop/bin/**, interop/idl/**, interop/peers/**, interop/approvals/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/conformance/idl-4.2-matrix.md, docs/conformance/idl-to-java-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g12-060-peer-idl-feature-interop-matrix.md, docs/roadmap/tasks/g12-100-durable-orb-poa-identity-design-gate.md, README.md
Forbidden files: committed peer binaries, committed live logs, committed raw live reports, optional service implementation, production runtime behavior unless a project-owned live peer defect is found and this task contract is amended before fixing it
Expected behavior: Task type: verification-only. Promote selected broad IDL feature fixtures to live black-box peer interop scenarios across approved peers, JVM and Native Image local lanes, and both local-client/peer-server and peer-client/local-server directions where each peer can support the fixture.
Tests to add/update: Peer harness tests for broad-feature scenario metadata, missing-prerequisite classification, report summary aggregation, peer capability filtering, and regression tests for any project-owned fixture adapters added in G12-050.
Documentation to update: Interop matrix, reference behavior capture notes, IDL and IDL-to-Java conformance matrices, roadmap index, README ready-task status, this task, and G12-100 status when complete.
Commands to run: ./gradlew :modules:corba-interop-testkit:test :modules:corba-native-image:test; ./interop/bin/interop-peer validate-manifests; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Approved broad-feature peer scenarios either pass or produce maintainer-reviewable structured classifications; project-owned live peer defects are split into amended implementation tasks before production fixes; raw live evidence remains ignored; G12-100 is promoted to ready-for-implementation when the compiler/interop hardening lane is closed.
Rollback notes: Revert peer harness, scenario metadata, report-summary, approval metadata, and documentation updates together.
