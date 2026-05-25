# G10-120-020 Local JVM Native Lane Commands

Task ID: G10-120-020-LOCAL-JVM-NATIVE-LANE-COMMANDS
Status: ready-for-implementation
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009, REQ-NATIVE-002
ADR IDs: ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0008, ADR-0010, ADR-0013
Specification references: CORBA-IF, CORBA-IOP, IDL-42, I2JAV-13, JAV2I-14, NAM-13
Target module: interop harness and Native Image smoke lanes
Allowed files: interop/**, modules/corba-interop-testkit/src/**, modules/corba-native-image/src/nativeSmoke/**, modules/corba-native-image/src/test/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-120-020-local-jvm-native-lane-commands.md, docs/roadmap/tasks/g10-120-030-peer-scenario-command-closure.md, README.md
Forbidden files: production behavior changes outside interop/native-smoke entrypoints, Gradle build logic changes, vendored peer source, committed peer binaries, optional service implementation, unapproved release version changes
Expected behavior: Task type: implementation. Add real local JVM and Native Image client/server lane commands for `run-direction-matrix`. Client lanes read `MJO_INTEROP_SERVER_IOR` and perform real remote calls. Server lanes bind on `0.0.0.0`, advertise a peer-container-reachable host, write `MJO_INTEROP_SERVER_IOR`, and stay alive until stopped.
Tests to add/update: Interop testkit coverage for local command environment, host-gateway handling, missing command reports, and local server IOR readiness; Native Image tests for JVM parity of the lane entrypoints.
Documentation to update: Interop matrix, reference behavior capture, pre-1.0 interop plan, roadmap index, README, this task, and G10-120-030 status when complete.
Commands to run: ./gradlew :modules:corba-interop-testkit:test :modules:corba-native-image:test; ./interop/bin/interop-peer validate-manifests; INTEROP_ARTIFACT_CACHE=/mnt/d/projects/mundane-java-orb/interop/work/artifact-cache ./interop/bin/interop-peer validate-gates --require-cache; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: `run-direction-matrix --require-live basic-idl ace-tao` can execute local JVM and Native Image lane commands when supplied, and missing local commands remain deterministic infrastructure reports.
Rollback notes: Revert local lane entrypoints, harness local-server reachability updates, and evidence documentation.
