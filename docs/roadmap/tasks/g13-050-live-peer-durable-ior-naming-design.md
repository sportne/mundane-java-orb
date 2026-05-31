# G13-050 Live Peer Durable IOR And Naming Design

Task ID: G13-050-LIVE-PEER-DURABLE-IOR-NAMING-DESIGN
Status: complete
Gate: G13 durable runtime hardening
Requirement IDs: REQ-IOR-002, REQ-NAM-001, REQ-INTEROP-001, REQ-INTEROP-002, REQ-INTEROP-003, REQ-INTEROP-004, REQ-INTEROP-005, REQ-INTEROP-006, REQ-INTEROP-007, REQ-INTEROP-008, REQ-INTEROP-009, REQ-NATIVE-002, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0003, ADR-0005, ADR-0006, ADR-0007, ADR-0008, ADR-0010, ADR-0014, ADR-0015
Specification references: CORBA-IOP-IOR, CORBA-IOP-IIOP, CORBA-IF-OBJECT-REF, NAM-13-SERVICE, NAM-13-URLS
Target module: interop design documentation
Allowed files: docs/verification/interop-matrix.md, docs/verification/reference-behavior-capture.md, docs/verification/pre-1-0-interoperability-plan.md, docs/architecture/runtime-architecture.md, docs/architecture/services-design.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g13-050-live-peer-durable-ior-naming-design.md, README.md
Forbidden files: production source, test source, Gradle build logic, peer artifacts, interop cache files, raw live reports, generated artifacts, optional service implementation
Expected behavior: Task type: design-only. Define future live peer durable persistence scenarios only as opaque object-key preservation claims, not peer understanding of the project-owned `MJOK` or `MJNS` formats; do not approve live execution.
Tests to add/update: No product tests; later approved implementation tasks must add peer harness metadata, missing-prerequisite classifications, structured reports, and local JVM/Native Image lane coverage.
Documentation to update: Interop matrix, reference behavior capture notes, pre-1.0 interoperability plan or successor interop plan, runtime and services architecture if claims are approved, roadmap index, README ready-task status, and this task.
Commands to run: ./gradlew validateDesignControlPack qualityGate; ./interop/bin/interop-peer validate-manifests; git diff --check
Acceptance criteria: The design names the proposed peer set, scenario names, direction matrix, cache/image prerequisites, structured report fields, expected failure classifications, and raw evidence policy; actual live durable IOR or persistent Naming execution remains unapproved and deferred to a later human-gated task.
Rollback notes: Revert live peer durable IOR/Naming design updates together.
