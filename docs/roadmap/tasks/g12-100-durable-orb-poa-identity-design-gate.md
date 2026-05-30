# G12-100 Durable ORB And POA Identity Design Gate

Task ID: G12-100-DURABLE-ORB-POA-IDENTITY-DESIGN-GATE
Status: ready-for-implementation
Gate: G12 post-1.0 runtime identity design
Requirement IDs: REQ-ORB-001, REQ-POA-001, REQ-POA-002, REQ-IOR-001, REQ-IOR-002, REQ-NAM-001, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0003, ADR-0005, ADR-0008, ADR-0010
Specification references: CORBA-IF-ORB, CORBA-IF-OBJECT-REF, CORBA-IF-POA, CORBA-IOP-IOR, NAM-13-SERVICE
Target module: documentation architecture only
Allowed files: docs/adr/ADR-*.md, docs/architecture/runtime-architecture.md, docs/architecture/poa-design.md, docs/architecture/cdr-giop-iiop.md, docs/architecture/services-design.md, docs/conformance/corba-3.4-matrix.md, docs/conformance/naming-service-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g12-100-durable-orb-poa-identity-design-gate.md, README.md
Forbidden files: production source, test source, Gradle build logic, generated artifacts, peer artifacts, interop live reports, optional service implementation
Expected behavior: Task type: design-only. Decide whether and how the project should support durable ORB identity, persistent POA identity, restart-safe object keys, persistent IORs, and Naming persistence after the compiler/interop G12 lane closes.
Tests to add/update: No product tests; define future unit, integration, interop, Native Image, and hostile-input verification expectations in the ADR and architecture documents.
Documentation to update: ADR record, runtime architecture, POA design, CDR/GIOP/IIOP architecture, services design if Naming persistence is in scope, conformance matrices, roadmap index, README ready-task status, and follow-on implementation tasks if approved.
Commands to run: ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Maintainers have an accepted or rejected ADR-level decision for durable identity; if accepted, follow-on implementation tasks cite requirements, allowed files, forbidden files, tests, documentation updates, Native Image posture, security constraints, and exact acceptance commands.
Rollback notes: Revert the durable identity ADR/design updates and any follow-on task files together.
