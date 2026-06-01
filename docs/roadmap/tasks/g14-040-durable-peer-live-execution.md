# G14-040 Durable Peer Live Execution

Task ID: G14-040-DURABLE-PEER-LIVE-EXECUTION
Status: complete
Gate: G14 durable peer persistence execution
Requirement IDs: REQ-IOR-002, REQ-NAM-001, REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0008, ADR-0010, ADR-0014, ADR-0015
Specification references: CORBA-IOP-IOR, CORBA-IOP-IIOP, CORBA-IF-OBJECT-REF, NAM-13-SERVICE, NAM-13-URLS
Target module: live interop durable peer execution
Allowed files: interop/**, modules/corba-interop-testkit/src/**, modules/corba-native-image/src/nativeSmoke/java/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/conformance/*.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g14-040-durable-peer-live-execution.md, docs/roadmap/tasks/g8-100-time-service-task-group.md, README.md
Forbidden files: production source, runtime behavior changes, Gradle build logic, vendored peer source, committed peer binaries, committed native binaries, raw live report outputs, Docker layers, downloaded artifacts, optional service implementation, unapproved release version changes
Expected behavior: Task type: implementation. G14-020 and G14-030 are complete, and maintainers approved live durable peer execution on 2026-05-31. Execute only the approved claim: peer clients preserving opaque object-key bytes when invoking our restarted servers or resolving our persistent Naming references after restart.
Tests to add/update: None unless live evidence validation gaps are found; later approved implementation may add report aggregation tests if required.
Documentation to update: Interop matrix, reference behavior capture, pre-1.0 interoperability plan or successor interop plan, conformance matrices if final evidence is accepted, roadmap index, README, and this task.
Commands to run: ./gradlew test; ./gradlew :modules:corba-native-image:nativeImageBinariesSmoke; ./gradlew validateDesignControlPack qualityGate; ./interop/bin/interop-peer validate-manifests; INTEROP_ARTIFACT_CACHE=/mnt/d/projects/mundane-java-orb/interop/work/artifact-cache ./interop/bin/interop-peer validate-gates --require-cache; ./interop/bin/interop-peer run-direction-matrix --require-live <durable-scenario> all; git diff --check
Acceptance criteria: Maintainers explicitly approve live execution; live reports cover JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB, and ACE/TAO for the approved durable IOR and persistent Naming directions; structured summaries classify every result; no raw logs, IORs, Naming stores, Docker layers, peer artifacts, native binaries, or downloaded artifacts are committed; peer claims do not require understanding `MJOK` or `MJNS`.
Rollback notes: Revert clean-room final evidence documentation and roadmap status updates; do not delete ignored raw local evidence unless explicitly requested.
