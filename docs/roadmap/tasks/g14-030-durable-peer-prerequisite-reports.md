# G14-030 Durable Peer Prerequisite Reports

Task ID: G14-030-DURABLE-PEER-PREREQUISITE-REPORTS
Status: ready-for-implementation
Gate: G14 durable peer persistence execution
Requirement IDs: REQ-IOR-002, REQ-NAM-001, REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0008, ADR-0010, ADR-0014, ADR-0015
Specification references: CORBA-IOP-IOR, CORBA-IOP-IIOP, CORBA-IF-OBJECT-REF, NAM-13-SERVICE, NAM-13-URLS
Target module: interop peer report validation
Allowed files: interop/**, modules/corba-interop-testkit/src/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g14-030-durable-peer-prerequisite-reports.md, README.md
Forbidden files: production source, runtime behavior changes, Gradle build logic, vendored peer source, committed peer binaries, raw live report outputs, Docker layers, native binaries, optional service implementation
Expected behavior: Task type: implementation. G14-010 accepted the local durable evidence and G14-020 added the durable peer metadata; add structured report validation for durable peer scenarios when required peer images, artifact cache entries, native binaries, Docker, or live approval are missing.
Tests to add/update: Interop testkit tests for missing-prerequisite reports, classification vocabulary, structured report fields, and exclusion of raw logs, IORs, Naming stores, peer artifacts, native binaries, Docker layers, and downloaded artifacts from committed evidence.
Documentation to update: Interop matrix, reference behavior capture, pre-1.0 interoperability plan or successor interop plan, roadmap index, README, and this task.
Commands to run: ./gradlew :modules:corba-interop-testkit:test; ./interop/bin/interop-peer validate-manifests; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: G14-010 acceptance and G14-020 completion have promoted this task from the inherited human gate to implementation-ready before execution; missing-prerequisite reports deterministically name the scenario, direction, peer, JVM/native lane, required artifact or runtime prerequisite, live-approval state, and expected classification; default gates do not run live peer execution; no raw live evidence is committed.
Rollback notes: Revert report-validation changes, tests, and documentation status updates together.
