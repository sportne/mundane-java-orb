# G14-020 Durable Peer Harness Metadata

Task ID: G14-020-DURABLE-PEER-HARNESS-METADATA
Status: human-gate-blocked
Gate: G14 durable peer persistence execution
Requirement IDs: REQ-IOR-002, REQ-NAM-001, REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0008, ADR-0010, ADR-0014, ADR-0015
Specification references: CORBA-IOP-IOR, CORBA-IOP-IIOP, CORBA-IF-OBJECT-REF, NAM-13-SERVICE, NAM-13-URLS
Target module: interop peer harness metadata
Allowed files: interop/**, modules/corba-interop-testkit/src/**, docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g14-020-durable-peer-harness-metadata.md, docs/roadmap/tasks/g14-030-durable-peer-prerequisite-reports.md, README.md
Forbidden files: production source, runtime behavior changes, Gradle build logic, vendored peer source, committed peer binaries, raw live report outputs, Docker layers, native binaries, optional service implementation
Expected behavior: Task type: human-gate-blocked. This task is held by the G14-010 evidence-acceptance gate, not by a separate human approval. After maintainers accept G13 local durable evidence, promote this task to `ready-for-implementation` and add dry-run harness metadata for durable IOR and persistent Naming peer scenarios without approving live execution. Scope every claim to peer preservation of opaque object-key bytes and ordinary IOR/Naming protocol behavior; peers must not parse or understand `MJOK` object keys or `MJNS` store files.
Tests to add/update: Interop testkit tests for scenario metadata, direction names, peer prerequisites, ignored raw-output paths, and dry-run validation for missing live prerequisites.
Documentation to update: Interop matrix, reference behavior capture, pre-1.0 interoperability plan or successor interop plan, roadmap index, README, and this task.
Commands to run: ./gradlew :modules:corba-interop-testkit:test; ./interop/bin/interop-peer validate-manifests; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: G14-010 acceptance has promoted this task from the inherited human gate to implementation-ready before execution; scenario metadata exists for JacORB, Eclipse GlassFish CORBA ORB, JBoss OpenJDK ORB, and ACE/TAO; candidate live directions cover peer clients invoking old durable IORs against restarted JVM and Native Image servers, plus peer clients resolving our persistent Naming references after restart; dry-run validation succeeds without requiring live peer execution; no raw live evidence is committed.
Rollback notes: Revert harness metadata, tests, and documentation status updates together.
