# G14-010 Local Durable Evidence Acceptance Gate

Task ID: G14-010-LOCAL-DURABLE-EVIDENCE-ACCEPTANCE-GATE
Status: complete
Gate: G14 durable peer persistence execution
Requirement IDs: REQ-IOR-002, REQ-NAM-001, REQ-POA-001, REQ-POA-002, REQ-ORB-001, REQ-INTEROP-009, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0003, ADR-0005, ADR-0008, ADR-0010, ADR-0014, ADR-0015
Specification references: CORBA-IF-ORB, CORBA-IF-POA, CORBA-IF-OBJECT-REF, CORBA-IOP-IOR, CORBA-IOP-IIOP, NAM-13-SERVICE, NAM-13-URLS
Target module: maintainer acceptance gate documentation
Allowed files: README.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g14-010-local-durable-evidence-acceptance-gate.md, docs/roadmap/tasks/g14-020-durable-peer-harness-metadata.md, docs/roadmap/tasks/g14-030-durable-peer-prerequisite-reports.md
Forbidden files: production source, test source, Gradle build logic, generated artifacts, peer artifacts, interop cache files, live interop reports, Docker layers, native binaries, optional service implementation
Expected behavior: Task type: human-gate-blocked. Maintainers review the completed G13 local durable evidence before any peer harness preparation becomes implementation-ready.
Tests to add/update: No product tests; later approved tasks may add harness metadata tests after this gate is accepted.
Documentation to update: This task, roadmap index, README ready-task status, and the next G14 task statuses if maintainers accept the local durable evidence.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Maintainers explicitly record whether local durable evidence is accepted for live peer preparation; if accepted, promote G14-020 to `ready-for-implementation` and keep G14-040 human-gated; if rejected, leave G14-020/G14-030 blocked and record the missing local evidence as future work.
Completion evidence: Completed on 2026-05-31 by explicit maintainer approval in the project thread. Maintainers accepted the completed G13 local durable evidence as sufficient to prepare live peer durable IOR/Naming harness metadata: cross-process durable IOR and persistent Naming restart evidence, durable POA path registration, adapter activation lookup, servant-manager rehydration, and IIOP durable-key routing are complete locally. The accepted claim remains local evidence only; live peer durable IOR/Naming execution remains human-gated under G14-040.
Rollback notes: Revert only the acceptance-status documentation changes; do not alter completed G13 implementation commits.
