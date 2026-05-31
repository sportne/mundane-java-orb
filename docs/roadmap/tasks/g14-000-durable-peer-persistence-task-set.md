# G14-000 Durable Peer Persistence Task Set

Task ID: G14-000-DURABLE-PEER-PERSISTENCE-TASK-SET
Status: complete
Gate: G14 durable peer persistence execution
Requirement IDs: REQ-IOR-002, REQ-NAM-001, REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0008, ADR-0010, ADR-0014, ADR-0015
Specification references: CORBA-IOP-IOR, CORBA-IOP-IIOP, CORBA-IF-OBJECT-REF, NAM-13-SERVICE, NAM-13-URLS
Target module: documentation roadmap only
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g14-*.md
Forbidden files: production source, test source, Gradle build logic, generated artifacts, peer artifacts, interop cache files, live interop reports, Docker layers, native binaries, optional service implementation
Expected behavior: Task type: design-only. Define the G14 durable peer persistence execution roadmap after G13 local durable routing work, with live execution blocked until maintainers explicitly accept the local durable IOR, POA routing, and persistent Naming evidence.
Tests to add/update: No product tests; validate the documentation control pack and peer manifest metadata.
Documentation to update: Add this task set, the G14 task sequence, README ready-task status, and roadmap index entries.
Commands to run: ./gradlew validateDesignControlPack qualityGate; ./interop/bin/interop-peer validate-manifests; git diff --check
Acceptance criteria: G14 task ordering is explicit; no non-human-gated G14 task is ready until local durable evidence is accepted; live peer execution is scoped to opaque object-key preservation only; peers are not expected to understand project-owned `MJOK` or `MJNS` formats; no runtime, generated-code, build, peer-cache, or live interop outputs are changed.
Rollback notes: Revert the G14 roadmap task files, README status update, and roadmap index update together.
